#!/usr/bin/env bash
# ============================================================
# 石室联中报修系统 · 服务器一键更新脚本
# 作用:拉最新代码 → 构建前端+后端 → 替换 app.jar → 重启服务 → 探活
# 用法:
#   chmod +x deploy/update.sh        # 首次赋可执行权限
#   sudo bash deploy/update.sh       # 每次更新执行这一条即可
# 前置(服务器需具备):git、node(18+)、/opt/jdk17、systemd 服务 ticket-system
# ============================================================
set -euo pipefail

# -------- 可按实际环境调整 --------
REPO_URL="https://github.com/maomaomcs/gongdan-system.git"
REPO_DIR="/opt/ticket-system/gongdan-system"   # 服务器上代码存放目录
APP_JAR="/opt/ticket-system/app.jar"           # systemd 里 ExecStart 指向的 jar
SERVICE="ticket-system"
JDK_HOME="/opt/jdk17"
HEALTH_URL="http://localhost:8082/api/config"
NPM_REGISTRY="https://registry.npmmirror.com"  # 国内镜像,加速
# ---------------------------------

log() { echo -e "\033[1;32m[$(date '+%H:%M:%S')] $*\033[0m"; }
err() { echo -e "\033[1;31m[错误] $*\033[0m" >&2; }

export JAVA_HOME="$JDK_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# 0. 环境检查
command -v git  >/dev/null 2>&1 || { err "未找到 git,请先安装:yum install -y git"; exit 1; }
command -v node >/dev/null 2>&1 || { err "未找到 node(前端构建需要),请安装 Node 18+ 后重试"; exit 1; }
[ -x "$JDK_HOME/bin/java" ]      || { err "未找到 JDK:$JDK_HOME/bin/java"; exit 1; }
log "环境检查通过 · node $(node -v) · java $($JDK_HOME/bin/java -version 2>&1 | head -n1)"

# 1. 拉取代码(部署机为纯构建用,强制与远端 main 对齐)
if [ -d "$REPO_DIR/.git" ]; then
  log "更新代码:$REPO_DIR"
  git -C "$REPO_DIR" fetch --all --prune
  git -C "$REPO_DIR" reset --hard origin/main
else
  log "首次克隆代码到:$REPO_DIR"
  mkdir -p "$(dirname "$REPO_DIR")"
  git clone "$REPO_URL" "$REPO_DIR"
fi
COMMIT=$(git -C "$REPO_DIR" rev-parse --short HEAD)
log "当前代码版本:$COMMIT"

# 2. 构建前端(产物输出到 backend/src/main/resources/static)
log "构建前端 ..."
cd "$REPO_DIR/frontend"
if [ -f package-lock.json ]; then
  npm ci --registry "$NPM_REGISTRY" || npm install --registry "$NPM_REGISTRY"
else
  npm install --registry "$NPM_REGISTRY"
fi
node node_modules/vite/bin/vite.js build
log "前端构建完成"

# 3. 构建后端(单 jar)
log "构建后端(首次会下载 maven/依赖,耐心等)..."
cd "$REPO_DIR/backend"
chmod +x mvnw || true
./mvnw -s settings.xml -q -DskipTests clean package
NEW_JAR="$REPO_DIR/backend/target/ticket-0.0.1-SNAPSHOT.jar"
[ -f "$NEW_JAR" ] || { err "构建产物不存在:$NEW_JAR"; exit 1; }
log "后端构建完成:$(du -h "$NEW_JAR" | cut -f1)"

# 4. 备份旧 jar 并替换
if [ -f "$APP_JAR" ]; then
  BAK="${APP_JAR}.bak.$(date '+%Y%m%d%H%M%S')"
  cp -f "$APP_JAR" "$BAK"
  log "已备份旧 jar → $BAK"
  # 只保留最近 5 个备份
  ls -1t "${APP_JAR}".bak.* 2>/dev/null | tail -n +6 | xargs -r rm -f
fi
cp -f "$NEW_JAR" "$APP_JAR"
log "已替换 → $APP_JAR"

# 5. 重启服务
log "重启服务 $SERVICE ..."
systemctl restart "$SERVICE"

# 6. 探活(最多等 30 秒)
log "等待服务就绪 ..."
ok=0
for i in $(seq 1 15); do
  sleep 2
  code=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || true)
  if [ "$code" = "200" ]; then ok=1; break; fi
done
if [ "$ok" = "1" ]; then
  log "✅ 更新成功!版本 $COMMIT,服务已就绪($HEALTH_URL 返回 200)"
else
  err "服务未在预期时间内就绪,请查看日志:journalctl -u $SERVICE -n 50 --no-pager"
  err "如需回滚:cp <最近的 .bak 文件> $APP_JAR && systemctl restart $SERVICE"
  exit 1
fi

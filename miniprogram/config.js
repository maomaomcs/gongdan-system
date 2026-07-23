// 全局配置
module.exports = {
  // 后端地址。开发阶段:微信开发者工具勾选「详情→本地设置→不校验合法域名」即可用 http 联调。
  // 上线前:改成已备案的 https 域名(如 https://bx.mm1.asia),并在小程序后台配置 request 合法域名。
  apiBase: 'http://43.136.56.131:8082',

  // 是否启用微信一键登录(需在后端配置 AppID/AppSecret 后置 true)
  wxLoginEnabled: false,
};

$ErrorActionPreference = 'Stop'
$base = 'http://localhost:8082'
function J($o){ $o | ConvertTo-Json -Depth 8 }

# 管理员登录
$tok = (Invoke-RestMethod -Uri "$base/api/login" -Method Post -ContentType 'application/json' -Body (J @{username='admin';password='admin123'})).token
$H = @{ Authorization = "Bearer $tok" }

# 造邀请码+注册老师+提交工单(待处理)
$inv = Invoke-RestMethod -Uri "$base/api/admin/invite-codes" -Method Post -Headers $H -ContentType 'application/json' -Body (J @{note='dingtest';maxUses=0})
$uname = "d" + (Get-Random -Maximum 99999)
$reg = Invoke-RestMethod -Uri "$base/api/user/register" -Method Post -ContentType 'application/json' -Body (J @{username=$uname;password='pass123';displayName='测试';inviteCode=$inv.code})
$UH = @{ Authorization = "Bearer $($reg.token)" }
$body = [System.Text.Encoding]::UTF8.GetBytes((J @{reporter='测试';location='测试室';category='网络';title='dingbug';urgency='普通'}))
$submit = Invoke-RestMethod -Uri "$base/api/user/tickets" -Method Post -Headers $UH -ContentType 'application/json; charset=utf-8' -Body $body
$tid = $submit.ticket.id
Write-Host "工单 id=$tid 状态=$($submit.ticket.status)"

# 老师取消 -> 已取消
$cancel = Invoke-RestMethod -Uri "$base/api/user/tickets/$tid/cancel" -Method Post -Headers $UH
Write-Host "取消后状态=$($cancel.status)"

# 读签名密钥
$secret = (& "..\tooling\mysql-8.0.29-winx64\bin\mysql.exe" -uroot --protocol=TCP -P3306 -N -B -e "SELECT setting_value FROM ticket_system.system_setting WHERE setting_key='ding.action-secret';").Trim()
Write-Host "密钥长度=$($secret.Length)"

function Sign($id,$action){
  $hmac = New-Object System.Security.Cryptography.HMACSHA256
  $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
  $h = $hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes("$id`:$action"))
  ($h | ForEach-Object { $_.ToString('x2') }) -join '' | ForEach-Object { $_.Substring(0,20) }
}

# 已取消后,尝试钉钉"认领"
$sig = Sign $tid 'claim'
$r = Invoke-WebRequest -Uri "$base/api/ding/act?t=$tid&a=claim&s=$sig" -UseBasicParsing
if ($r.Content -match '操作成功') { Write-Host "[认领] BUG复现: 已取消工单竟能被认领!" }
elseif ($r.Content -match '操作失败|无需再处理') { Write-Host "[认领] 已正确拦截" }
else { Write-Host "[认领] 未知响应" }

# 已取消后,尝试钉钉"已解决"
$sig2 = Sign $tid 'resolve'
$r2 = Invoke-WebRequest -Uri "$base/api/ding/act?t=$tid&a=resolve&s=$sig2" -UseBasicParsing
if ($r2.Content -match '操作成功') { Write-Host "[已解决] BUG复现: 已取消工单竟能被标记已解决!" }
elseif ($r2.Content -match '操作失败|无需再处理') { Write-Host "[已解决] 已正确拦截" }
else { Write-Host "[已解决] 未知响应" }

# 查询最终状态
$final = Invoke-RestMethod -Uri "$base/api/admin/tickets/$tid" -Headers $H
Write-Host "最终状态=$($final.status)"

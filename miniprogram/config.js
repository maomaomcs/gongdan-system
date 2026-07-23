// 全局配置
module.exports = {
  // 后端地址(已配 HTTPS)。真机/体验版/正式版用此 https 域名。
  // 开发者工具里仍需在小程序后台「服务器域名」把 https://bx.mm1.asia 加入 request/uploadFile 合法域名,
  // 或在「详情→本地设置」勾「不校验合法域名」联调。
  apiBase: 'https://bx.mm1.asia',

  // 是否启用微信一键登录(需在后端配置 AppID/AppSecret 后置 true)
  wxLoginEnabled: false,
};

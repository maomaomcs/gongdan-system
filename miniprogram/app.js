App({
  globalData: {
    userInfo: null,
  },
  onLaunch() {
    // 预留:检查登录态由各页面 onShow 处理
  },
  isLogin() {
    return !!wx.getStorageSync('user_token');
  },
});

const api = require('../../utils/api.js');
const config = require('../../config.js');

Page({
  data: {
    mode: 'login', // login | register
    username: '',
    password: '',
    displayName: '',
    phone: '',
    inviteCode: '',
    loading: false,
    wxLoginEnabled: config.wxLoginEnabled,
  },
  onInput(e) {
    this.setData({ [e.currentTarget.dataset.field]: e.detail.value });
  },
  switchMode(e) {
    const m = e.currentTarget.dataset.m;
    this.setData({ mode: m === 'register' ? 'register' : 'login' });
  },
  async submit() {
    const d = this.data;
    if (!d.username || !d.password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }
    if (d.mode === 'register' && !d.inviteCode) {
      wx.showToast({ title: '注册需要邀请码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const path = d.mode === 'login' ? '/api/user/login' : '/api/user/register';
      const body = d.mode === 'login'
        ? { username: d.username, password: d.password }
        : { username: d.username, password: d.password, displayName: d.displayName, phone: d.phone, inviteCode: d.inviteCode };
      const res = await api.post(path, body, { noAuth: true });
      wx.setStorageSync('user_token', res.token);
      wx.setStorageSync('display_name', res.displayName || res.username);
      wx.showToast({ title: d.mode === 'login' ? '登录成功' : '注册成功' });
      setTimeout(() => wx.switchTab({ url: '/pages/report/report' }), 600);
    } catch (e) {
      // api 已提示错误
    } finally {
      this.setData({ loading: false });
    }
  },
});

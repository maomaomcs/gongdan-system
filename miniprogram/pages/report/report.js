const api = require('../../utils/api.js');
const app = getApp();

Page({
  data: {
    categories: [],
    urgencies: ['普通', '紧急'],
    locations: [],
    faqs: [],
    form: { category: '', location: '', title: '', description: '', urgency: '普通' },
    images: [],      // [{ url, full }]
    uploading: false,
    submitting: false,
    faqOpen: false,
  },
  onShow() {
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    if (this.data.categories.length === 0) this.loadConfig();
  },
  async loadConfig() {
    try {
      const c = await api.get('/api/config', { noAuth: true, silent: true });
      this.setData({
        categories: c.categories || [],
        urgencies: c.urgencies && c.urgencies.length ? c.urgencies : ['普通', '紧急'],
        locations: c.locations || [],
        faqs: c.faqs || [],
      });
    } catch (e) {}
  },
  onField(e) {
    this.setData({ ['form.' + e.currentTarget.dataset.field]: e.detail.value });
  },
  onCategory(e) {
    this.setData({ 'form.category': this.data.categories[e.detail.value] });
  },
  onUrgency(e) {
    this.setData({ 'form.urgency': this.data.urgencies[e.detail.value] });
  },
  pickLocation(e) {
    this.setData({ 'form.location': e.currentTarget.dataset.loc });
  },
  toggleFaq() {
    this.setData({ faqOpen: !this.data.faqOpen });
  },
  async chooseImage() {
    if (this.data.images.length >= 6) {
      wx.showToast({ title: '最多6张', icon: 'none' });
      return;
    }
    try {
      const res = await wx.chooseMedia({ count: 6 - this.data.images.length, mediaType: ['image'], sizeType: ['compressed'] });
      this.setData({ uploading: true });
      const arr = this.data.images.slice();
      for (const f of res.tempFiles) {
        const url = await api.uploadImage(f.tempFilePath);
        arr.push({ url: url, full: api.fullUrl(url) });
      }
      this.setData({ images: arr });
    } catch (e) {
      wx.showToast({ title: '上传失败', icon: 'none' });
    } finally {
      this.setData({ uploading: false });
    }
  },
  previewImage(e) {
    const cur = e.currentTarget.dataset.full;
    wx.previewImage({ current: cur, urls: this.data.images.map((x) => x.full) });
  },
  removeImage(e) {
    const i = e.currentTarget.dataset.i;
    const arr = this.data.images.slice();
    arr.splice(i, 1);
    this.setData({ images: arr });
  },
  async submit() {
    const f = this.data.form;
    if (!f.category) { wx.showToast({ title: '请选择故障类型', icon: 'none' }); return; }
    if (!f.location) { wx.showToast({ title: '请填写位置', icon: 'none' }); return; }
    if (!f.title) { wx.showToast({ title: '请填写问题简述', icon: 'none' }); return; }
    this.setData({ submitting: true });
    try {
      const reporter = wx.getStorageSync('display_name') || '老师';
      const res = await api.post('/api/user/tickets', {
        reporter: reporter,
        location: f.location,
        category: f.category,
        title: f.title,
        description: f.description,
        urgency: f.urgency,
        images: this.data.images.map((x) => x.url),
      });
      wx.showModal({
        title: '提交成功',
        content: '工单号:' + res.code + '，可在「我的报修」查看进度',
        showCancel: false,
        success: () => {
          this.setData({ form: { category: '', location: '', title: '', description: '', urgency: '普通' }, images: [] });
          wx.switchTab({ url: '/pages/my/my' });
        },
      });
    } catch (e) {
    } finally {
      this.setData({ submitting: false });
    }
  },
});

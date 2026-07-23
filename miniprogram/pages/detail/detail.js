const api = require('../../utils/api.js');
const app = getApp();

Page({
  data: {
    id: null,
    t: null,
    images: [],   // full urls
    logs: [],
    canUrge: false,
    canCancel: false,
    acting: false,
  },
  onLoad(q) {
    this.setData({ id: q.id });
  },
  onShow() {
    if (!app.isLogin()) { wx.reLaunch({ url: '/pages/login/login' }); return; }
    this.load();
  },
  async load() {
    try {
      const t = await api.get('/api/user/tickets/' + this.data.id);
      const images = (t.images || []).map(api.fullUrl);
      const logs = (t.logs || []).map((l) => ({
        content: l.content,
        author: l.author,
        time: (l.createdAt || '').substring(0, 16),
      }));
      const active = t.status === '待处理' || t.status === '处理中';
      this.setData({
        t: {
          id: t.id, code: t.code, title: t.title, category: t.category, location: t.location,
          description: t.description, urgency: t.urgency, status: t.status,
          handler: t.handler, resolution: t.resolution,
          date: (t.createdAt || '').substring(0, 16),
          urgeCount: t.urgeCount || 0,
        },
        images,
        logs,
        canUrge: active,
        canCancel: t.status === '待处理',
      });
    } catch (e) {}
  },
  preview(e) {
    wx.previewImage({ current: e.currentTarget.dataset.url, urls: this.data.images });
  },
  async urge() {
    if (this.data.acting) return;
    this.setData({ acting: true });
    try {
      await api.post('/api/user/tickets/' + this.data.id + '/urge');
      wx.showToast({ title: '已催单', icon: 'success' });
      this.load();
    } catch (e) {} finally { this.setData({ acting: false }); }
  },
  cancel() {
    wx.showModal({
      title: '取消报修',
      content: '确定取消该报修吗?仅“待处理”时可取消。',
      confirmColor: '#a4232a',
      success: async (r) => {
        if (!r.confirm) return;
        this.setData({ acting: true });
        try {
          await api.post('/api/user/tickets/' + this.data.id + '/cancel');
          wx.showToast({ title: '已取消', icon: 'success' });
          this.load();
        } catch (e) {} finally { this.setData({ acting: false }); }
      },
    });
  },
});

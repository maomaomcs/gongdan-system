const api = require('../../utils/api.js');
const app = getApp();

const FILTERS = [
  { key: '', label: '全部' },
  { key: '待处理', label: '待处理' },
  { key: '处理中', label: '处理中' },
  { key: '已解决', label: '已解决' },
  { key: '已取消', label: '已取消' },
];

Page({
  data: {
    filters: FILTERS,
    status: '',
    list: [],
    page: 0,
    pages: 1,
    loading: false,
  },
  onShow() {
    if (!app.isLogin()) {
      wx.reLaunch({ url: '/pages/login/login' });
      return;
    }
    this.reload();
  },
  onPullDownRefresh() {
    this.reload(() => wx.stopPullDownRefresh());
  },
  onReachBottom() {
    if (this.data.page + 1 < this.data.pages && !this.data.loading) {
      this.loadPage(this.data.page + 1);
    }
  },
  reload(done) {
    this.setData({ list: [], page: 0, pages: 1 });
    this.loadPage(0, done);
  },
  chooseFilter(e) {
    this.setData({ status: e.currentTarget.dataset.key });
    this.reload();
  },
  async loadPage(page, done) {
    this.setData({ loading: true });
    try {
      const q = '/api/user/tickets?page=' + page + '&size=10' + (this.data.status ? '&status=' + encodeURIComponent(this.data.status) : '');
      const res = await api.get(q);
      const rows = (res.list || []).map((t) => ({
        id: t.id,
        code: t.code,
        title: t.title,
        category: t.category,
        location: t.location,
        status: t.status,
        urgency: t.urgency,
        urgeCount: t.urgeCount || 0,
        userUnread: t.userUnread,
        overdue: t.overdue,
        date: (t.createdAt || '').substring(0, 16),
      }));
      this.setData({
        list: page === 0 ? rows : this.data.list.concat(rows),
        page: res.page != null ? res.page : page,
        pages: res.pages != null ? res.pages : 1,
      });
    } catch (e) {
    } finally {
      this.setData({ loading: false });
      if (done) done();
    }
  },
  goDetail(e) {
    wx.navigateTo({ url: '/pages/detail/detail?id=' + e.currentTarget.dataset.id });
  },
});

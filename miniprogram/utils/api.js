const config = require('../config.js');

function token() {
  return wx.getStorageSync('user_token') || '';
}

function fullUrl(p) {
  if (!p) return '';
  if (/^https?:\/\//.test(p)) return p;
  return config.apiBase + p;
}

// 统一请求。返回 Promise;非 2xx 抛错(带后端 error 文案);401 自动回登录页。
function request(method, path, data, opts) {
  opts = opts || {};
  return new Promise((resolve, reject) => {
    const header = { 'Content-Type': 'application/json' };
    if (!opts.noAuth && token()) header['Authorization'] = 'Bearer ' + token();
    wx.request({
      url: config.apiBase + path,
      method: method,
      data: data || {},
      header: header,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data);
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('user_token');
          wx.showToast({ title: '登录已过期', icon: 'none' });
          setTimeout(() => wx.reLaunch({ url: '/pages/login/login' }), 800);
          reject(res.data || { error: '未登录' });
        } else {
          const msg = (res.data && res.data.error) || '请求失败(' + res.statusCode + ')';
          if (!opts.silent) wx.showToast({ title: msg, icon: 'none' });
          reject(res.data || { error: msg });
        }
      },
      fail(err) {
        if (!opts.silent) wx.showToast({ title: '网络异常', icon: 'none' });
        reject(err);
      },
    });
  });
}

// 上传单张图片,返回后端 url(形如 /api/files/xxx)
function uploadImage(filePath) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: config.apiBase + '/api/upload',
      filePath: filePath,
      name: 'file',
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300) {
          try {
            const d = JSON.parse(res.data);
            resolve(d.url);
          } catch (e) { reject({ error: '上传返回解析失败' }); }
        } else {
          reject({ error: '上传失败(' + res.statusCode + ')' });
        }
      },
      fail(err) { reject(err); },
    });
  });
}

module.exports = {
  request,
  uploadImage,
  fullUrl,
  get: (p, opts) => request('GET', p, null, opts),
  post: (p, data, opts) => request('POST', p, data, opts),
};

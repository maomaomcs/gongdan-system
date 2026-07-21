// 简单的 Service Worker:让应用可安装 + 静态资源缓存(应用外壳)
// 策略:导航/接口走网络优先(保证数据最新),静态资源(js/css/图片)缓存优先
const CACHE = 'ticket-pwa-v1';

self.addEventListener('install', (e) => {
  self.skipWaiting();
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    ).then(() => self.clients.claim())
  );
});

self.addEventListener('fetch', (e) => {
  const req = e.request;
  if (req.method !== 'GET') return;
  const url = new URL(req.url);

  // 接口与上传文件:始终走网络(不缓存动态数据)
  if (url.pathname.startsWith('/api/')) return;

  // 页面导航:网络优先,失败回退缓存的首页(离线也能打开壳)
  if (req.mode === 'navigate') {
    e.respondWith(
      fetch(req).catch(() => caches.match('/index.html'))
    );
    return;
  }

  // 静态资源:缓存优先,同时后台更新
  e.respondWith(
    caches.match(req).then((cached) => {
      const fetchPromise = fetch(req).then((res) => {
        if (res && res.status === 200 && (url.pathname.startsWith('/assets/') || /\.(js|css|png|svg|ico|woff2?)$/.test(url.pathname))) {
          const clone = res.clone();
          caches.open(CACHE).then((c) => c.put(req, clone));
        }
        return res;
      }).catch(() => cached);
      return cached || fetchPromise;
    })
  );
});

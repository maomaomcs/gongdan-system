const WEB = 'http://localhost:8080';
const { chromium } = require('d:/security agent/balance-checker/node_modules/playwright');
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1440, height: 860 }, deviceScaleFactor: 1.5 });
  const p = await ctx.newPage();
  p.on('pageerror', e => console.log('PAGEERR:', e.message));

  await p.goto(WEB + '/login', { waitUntil: 'networkidle' });
  await p.waitForSelector('input', { timeout: 10000 });
  await p.waitForTimeout(800);
  await p.screenshot({ path: 'shot-login2.png' });

  // 登录
  const inputs = await p.$$('input');
  console.log('inputs on login =', inputs.length);
  await inputs[0].fill('admin');          // 用户名
  await inputs[1].fill('admin123');       // 密码
  await p.click('button');
  await p.waitForTimeout(1500);

  // 账号管理页
  await p.goto(WEB + '/admin/users', { waitUntil: 'networkidle' });
  await p.waitForTimeout(1200);
  await p.screenshot({ path: 'shot-users.png', fullPage: true });

  // 打开新增账号弹窗
  const btns = await p.$$('button');
  for (const btn of btns) { const t = await btn.innerText(); if (t.includes('新增账号')) { await btn.click(); break; } }
  await p.waitForTimeout(700);
  await p.screenshot({ path: 'shot-adduser.png' });

  await b.close();
  console.log('SHOT_DONE');
})().catch(e => { console.error(e); process.exit(1); });

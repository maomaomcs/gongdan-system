import { ref } from 'vue'
import { getUnreadCount } from '../api'

// 模块级共享:多个组件共用同一个未读计数
const count = ref(0)

export function useUnread() {
  async function refresh() {
    if (!localStorage.getItem('user_token')) { count.value = 0; return }
    try {
      const r = await getUnreadCount()
      count.value = r.count || 0
    } catch (e) { /* 忽略 */ }
  }
  return { count, refresh }
}

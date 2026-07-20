import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截:附带 token
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ticket_token')
  if (token) config.headers.Authorization = 'Bearer ' + token
  return config
})

// 响应拦截:统一错误处理
api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.error || err.message || '请求失败'
    if (status === 401) {
      // 未授权:清除并跳登录(公开接口的 401 是密码错误,交由页面处理)
      if (!err.config.url.includes('/login')) {
        localStorage.removeItem('ticket_token')
        if (router.currentRoute.value.path.startsWith('/admin')) {
          router.push('/login')
        }
      }
    }
    return Promise.reject(new Error(msg))
  }
)

export default api

// ---- 具体接口 ----
export const getConfig = () => api.get('/config')
export const createTicket = (data) => api.post('/tickets', data)
export const queryByCode = (code) => api.get('/tickets/code/' + encodeURIComponent(code))
export const login = (username, password) => api.post('/login', { username, password })

// 账号相关
export const getMe = () => api.get('/admin/me')
export const logout = () => api.post('/admin/logout')
export const changePassword = (oldPassword, newPassword) => api.post('/admin/change-password', { oldPassword, newPassword })
export const listUsers = () => api.get('/admin/users')
export const createUser = (data) => api.post('/admin/users', data)
export const setUserEnabled = (id, enabled) => api.patch('/admin/users/' + id, { enabled })
export const deleteUser = (id) => api.delete('/admin/users/' + id)

// 系统设置 - 钉钉通知
export const getDingSettings = () => api.get('/admin/settings/ding')
export const saveDingSettings = (data) => api.put('/admin/settings/ding', data)
export const testDingNotify = () => api.post('/admin/settings/ding/test')

export const listTickets = (params) => api.get('/admin/tickets', { params })
export const getTicket = (id) => api.get('/admin/tickets/' + id)
export const updateTicket = (id, data) => api.patch('/admin/tickets/' + id, data)
export const addLog = (id, data) => api.post('/admin/tickets/' + id + '/logs', data)
export const getStats = () => api.get('/admin/stats')

// 导出 Excel(带筛选条件),返回 blob 并触发下载
export async function exportTicketsExcel(params) {
  const token = localStorage.getItem('ticket_token')
  const qs = new URLSearchParams(params).toString()
  const res = await fetch('/api/admin/tickets/export' + (qs ? '?' + qs : ''), {
    headers: { Authorization: 'Bearer ' + token },
  })
  if (!res.ok) throw new Error('导出失败')
  const blob = await res.blob()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '报修工单_' + new Date().toISOString().slice(0, 10) + '.xlsx'
  document.body.appendChild(a)
  a.click()
  a.remove()
  URL.revokeObjectURL(url)
}

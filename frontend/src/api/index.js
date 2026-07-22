import axios from 'axios'
import router from '../router'

const api = axios.create({ baseURL: '/api', timeout: 15000 })

// 根据请求路径选择对应的 token(管理端 / 用户端)
api.interceptors.request.use((config) => {
  const url = config.url || ''
  if (url.startsWith('/admin')) {
    const t = localStorage.getItem('admin_token')
    if (t) config.headers.Authorization = 'Bearer ' + t
  } else if (url.startsWith('/user') && !url.includes('/user/login') && !url.includes('/user/register')) {
    const t = localStorage.getItem('user_token')
    if (t) config.headers.Authorization = 'Bearer ' + t
  }
  return config
})

api.interceptors.response.use(
  (res) => res.data,
  (err) => {
    const status = err.response?.status
    const msg = err.response?.data?.error || err.message || '请求失败'
    const url = err.config?.url || ''
    if (status === 401) {
      if (url.startsWith('/admin')) {
        localStorage.removeItem('admin_token')
        if (router.currentRoute.value.path.startsWith('/admin')) router.push('/admin/login')
      } else if (url.startsWith('/user') && !url.includes('/login') && !url.includes('/register')) {
        localStorage.removeItem('user_token')
        if (!router.currentRoute.value.path.startsWith('/admin')) router.push('/login')
      }
    }
    return Promise.reject(new Error(msg))
  }
)

export default api

// ---- 公开 ----
export const getConfig = () => api.get('/config')
export const queryByCode = (code) => api.get('/tickets/code/' + encodeURIComponent(code))

// ---- 用户(老师)端 ----
export const userRegister = (data) => api.post('/user/register', data)
export const userLogin = (username, password) => api.post('/user/login', { username, password })
export const userMe = () => api.get('/user/me')
export const userLogout = () => api.post('/user/logout')
export const submitTicket = (data) => api.post('/user/tickets', data)
export const getMyTickets = (params) => api.get('/user/tickets', { params })
export const getMyTicket = (id) => api.get('/user/tickets/' + id)
export const urgeTicket = (id) => api.post('/user/tickets/' + id + '/urge')
export const cancelTicket = (id) => api.post('/user/tickets/' + id + '/cancel')

// ---- 管理端登录/账号 ----
export const adminLogin = (username, password) => api.post('/login', { username, password })
export const getMe = () => api.get('/admin/me')
export const logout = () => api.post('/admin/logout')
export const changePassword = (oldPassword, newPassword) => api.post('/admin/change-password', { oldPassword, newPassword })
export const listUsers = () => api.get('/admin/users')
export const createUser = (data) => api.post('/admin/users', data)
export const setUserEnabled = (id, enabled) => api.patch('/admin/users/' + id, { enabled })
export const deleteUser = (id) => api.delete('/admin/users/' + id)

// ---- 管理端:老师账号管理 ----
export const listAppUsers = (params) => api.get('/admin/app-users', { params })
export const setAppUserEnabled = (id, enabled) => api.patch('/admin/app-users/' + id, { enabled })
export const resetAppUserPassword = (id, newPassword) => api.post('/admin/app-users/' + id + '/reset-password', { newPassword })
export const promoteAppUser = (id) => api.post('/admin/app-users/' + id + '/promote')
export const deleteAppUser = (id) => api.delete('/admin/app-users/' + id)

// ---- 管理端:注册邀请码 ----
export const listInviteCodes = (params) => api.get('/admin/invite-codes', { params })
export const createInviteCode = (data) => api.post('/admin/invite-codes', data)
export const setInviteCodeEnabled = (id, enabled) => api.patch('/admin/invite-codes/' + id, { enabled })
export const deleteInviteCode = (id) => api.delete('/admin/invite-codes/' + id)

// ---- 管理端工单/统计/设置 ----
export const listTickets = (params) => api.get('/admin/tickets', { params })
export const getTicket = (id) => api.get('/admin/tickets/' + id)
export const updateTicket = (id, data) => api.patch('/admin/tickets/' + id, data)
export const addLog = (id, data) => api.post('/admin/tickets/' + id + '/logs', data)
export const getStats = () => api.get('/admin/stats')
export const getDingSettings = () => api.get('/admin/settings/ding')
export const saveDingSettings = (data) => api.put('/admin/settings/ding', data)
export const testDingNotify = () => api.post('/admin/settings/ding/test')
export const getOptions = () => api.get('/admin/settings/options')
export const saveOptions = (data) => api.put('/admin/settings/options', data)

export async function exportTicketsExcel(params) {
  const token = localStorage.getItem('admin_token')
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
  document.body.appendChild(a); a.click(); a.remove()
  URL.revokeObjectURL(url)
}

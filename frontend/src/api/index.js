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

export const listTickets = (params) => api.get('/admin/tickets', { params })
export const getTicket = (id) => api.get('/admin/tickets/' + id)
export const updateTicket = (id, data) => api.patch('/admin/tickets/' + id, data)
export const addLog = (id, data) => api.post('/admin/tickets/' + id + '/logs', data)
export const getStats = () => api.get('/admin/stats')

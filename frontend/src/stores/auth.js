import { defineStore } from 'pinia'
import api from '../api'

export const useAuth = defineStore('auth', {
  state: () => ({ user: null, token: localStorage.getItem('token'), ready: false }),
  getters: {
    loggedIn: s => !!s.user,
    isAdmin: s => s.user?.role === 'ADMIN',
    isStaff: s => s.user?.role === 'ADMIN' || s.user?.role === 'STAFF'
  },
  actions: {
    setSession(token, user) {
      this.token = token; this.user = user
      localStorage.setItem('token', token)
      if (user) localStorage.setItem('user', JSON.stringify(user))
    },
    updateUser(user) {
      this.user = { ...this.user, ...user }
      localStorage.setItem('user', JSON.stringify(this.user))
    },
    async init() {
      const stored = localStorage.getItem('user')
      if (stored) {
        try { this.user = JSON.parse(stored) } catch {}
      }
      if (this.token) {
        try {
          this.user = (await api.get('/auth/me')).data
          localStorage.setItem('user', JSON.stringify(this.user))
        } catch { this.logout() }
      }
      this.ready = true
    },
    async login(email, password, totp) {
      const { data } = await api.post('/auth/login', { email, password, totp: totp || undefined })
      if (data.totpRequired) return 'TOTP'
      this.setSession(data.token, data.user)
      return data
    },
    async register(form) {
      const { data } = await api.post('/auth/register', form)
      this.setSession(data.token, data.user)
      return data
    },
    async loginWithToken(token) {
      localStorage.setItem('token', token); this.token = token; this.user = null
      await this.init()
    },
    logout() {
      this.user = null; this.token = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})

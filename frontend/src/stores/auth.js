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
    },
    async init() {
      if (this.token && !this.user) {
        try { this.user = (await api.get('/auth/me')).data } catch { this.logout() }
      }
      this.ready = true
    },
    async login(email, password) {
      const { data } = await api.post('/auth/login', { email, password })
      this.setSession(data.token, data.user)
    },
    async register(form) {
      const { data } = await api.post('/auth/register', form)
      this.setSession(data.token, data.user)
    },
    async loginWithToken(token) {
      localStorage.setItem('token', token); this.token = token; this.user = null
      await this.init()
    },
    logout() {
      this.user = null; this.token = null
      localStorage.removeItem('token')
    }
  }
})

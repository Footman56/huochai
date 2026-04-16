import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login, logout, getUserInfo, refreshToken, type LoginParams, type UserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
  const refreshTokenValue = ref<string>(localStorage.getItem('refreshToken') || '')
  const userInfo = ref<UserInfo | null>(null)

  // 登录
  async function handleLogin(params: LoginParams) {
    try {
      const res = await login(params)
      token.value = res.accessToken
      refreshTokenValue.value = res.refreshToken
      localStorage.setItem('token', res.accessToken)
      localStorage.setItem('refreshToken', res.refreshToken)
      return res
    } catch (error) {
      throw error
    }
  }

  // 登出
  async function handleLogout() {
    try {
      await logout()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      clearAuth()
    }
  }

  // 清除认证信息
  function clearAuth() {
    token.value = ''
    refreshTokenValue.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
  }

  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const res = await getUserInfo()
      userInfo.value = res
      return res
    } catch (error) {
      throw error
    }
  }

  // 刷新 Token
  async function handleRefreshToken() {
    try {
      const res = await refreshToken({ refreshToken: refreshTokenValue.value })
      token.value = res.accessToken
      refreshTokenValue.value = res.refreshToken
      localStorage.setItem('token', res.accessToken)
      localStorage.setItem('refreshToken', res.refreshToken)
      return res
    } catch (error) {
      clearAuth()
      throw error
    }
  }

  return {
    token,
    refreshTokenValue,
    userInfo,
    handleLogin,
    handleLogout,
    clearAuth,
    fetchUserInfo,
    handleRefreshToken
  }
})
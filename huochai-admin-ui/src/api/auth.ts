import { get, post } from '@/utils/request'

// 登录参数
export interface LoginParams {
  username: string
  password: string
  clientType?: string
  deviceId?: string
  captchaUuid?: string
  captchaCode?: string
}

// 登录响应
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  sessionId: string
  userId: number
  username: string
}

// 用户信息
export interface UserInfo {
  userId: number
  username: string
  email?: string
  phone?: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

// 验证码响应
export interface CaptchaResponse {
  uuid: string
  image: string
}

// 刷新 Token 参数
export interface RefreshTokenParams {
  refreshToken: string
  clientType?: string
  deviceId?: string
}

/**
 * 获取验证码
 */
export function getCaptcha(): Promise<CaptchaResponse> {
  return get('/auth/captcha')
}

/**
 * 登录
 */
export function login(params: LoginParams): Promise<LoginResponse> {
  return post('/auth/login', params)
}

/**
 * 登出
 */
export function logout(): Promise<void> {
  return post('/auth/logout')
}

/**
 * 刷新 Token
 */
export function refreshToken(params: RefreshTokenParams): Promise<LoginResponse> {
  return post('/auth/refresh', params)
}

/**
 * 获取当前用户信息
 */
export function getUserInfo(): Promise<UserInfo> {
  return get('/auth/user-info')
}
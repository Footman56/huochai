<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>火柴后台管理系统</h2>
        <p>完整登录体系 - Redis + RBAC</p>
      </div>
      
      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="Lock"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input
              v-model="loginForm.captchaCode"
              placeholder="请输入验证码"
              prefix-icon="Key"
              size="large"
              @keyup.enter="handleLogin"
            />
            <div class="captcha-img" @click="refreshCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
              <span v-else>点击获取</span>
            </div>
          </div>
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <p>默认账号: admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { getCaptcha, type CaptchaResponse } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 加载状态
const loading = ref(false)

// 验证码图片
const captchaImage = ref('')
const captchaUuid = ref('')

// 登录表单
const loginForm = reactive({
  username: '',
  password: '',
  captchaCode: '',
  clientType: 'WEB',
  deviceId: 'web-admin'
})

// 表单验证规则
const loginRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  captchaCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

// 获取验证码
async function refreshCaptcha() {
  try {
    const res: CaptchaResponse = await getCaptcha()
    captchaImage.value = 'data:image/png;base64,' + res.image
    captchaUuid.value = res.uuid
  } catch (error) {
    console.error('获取验证码失败:', error)
  }
}

// 登录
async function handleLogin() {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    
    try {
      await userStore.handleLogin({
        ...loginForm,
        captchaUuid: captchaUuid.value
      })
      
      ElMessage.success('登录成功')
      
      // 获取用户信息
      await userStore.fetchUserInfo()
      
      // 跳转到首页
      router.push('/')
    } catch (error: any) {
      // 登录失败，刷新验证码
      refreshCaptcha()
      loginForm.captchaCode = ''
    } finally {
      loading.value = false
    }
  })
}

// 初始化
onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
  
  h2 {
    margin: 0;
    color: #333;
    font-size: 24px;
  }
  
  p {
    margin: 10px 0 0;
    color: #999;
    font-size: 14px;
  }
}

.login-form {
  .captcha-row {
    display: flex;
    gap: 10px;
    
    .el-input {
      flex: 1;
    }
    
    .captcha-img {
      width: 120px;
      height: 40px;
      border: 1px solid #dcdfe6;
      border-radius: 4px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      overflow: hidden;
      
      img {
        width: 100%;
        height: 100%;
        object-fit: contain;
      }
      
      span {
        color: #999;
        font-size: 12px;
      }
      
      &:hover {
        border-color: #409eff;
      }
    }
  }
  
  .login-btn {
    width: 100%;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  
  p {
    margin: 0;
    color: #999;
    font-size: 12px;
  }
}
</style>
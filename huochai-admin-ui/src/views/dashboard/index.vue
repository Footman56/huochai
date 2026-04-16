<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <!-- 欢迎卡片 -->
      <el-col :span="24">
        <el-card class="welcome-card">
          <div class="welcome-content">
            <h2>欢迎回来，{{ userStore.userInfo?.username }}</h2>
            <p>上次登录时间：{{ lastLoginTime }}</p>
          </div>
        </el-card>
      </el-col>
      
      <!-- 统计卡片 -->
      <el-col :span="6" v-for="item in statsCards" :key="item.key">
        <el-card class="stats-card" :body-style="{ padding: '20px' }">
          <div class="stats-content">
            <div class="stats-icon" :style="{ backgroundColor: item.color }">
              <el-icon :size="30"><component :is="item.icon" /></el-icon>
            </div>
            <div class="stats-info">
              <div class="stats-value">{{ item.value }}</div>
              <div class="stats-label">{{ item.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 快捷操作 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>快捷操作</span>
          </template>
          <div class="quick-actions">
            <el-button type="primary" @click="$router.push('/system/user')">
              <el-icon><User /></el-icon>
              用户管理
            </el-button>
            <el-button type="success" @click="$router.push('/system/role')">
              <el-icon><UserFilled /></el-icon>
              角色管理
            </el-button>
            <el-button type="warning" @click="$router.push('/system/permission')">
              <el-icon><Lock /></el-icon>
              权限管理
            </el-button>
            <el-button type="info" @click="$router.push('/system/login-log')">
              <el-icon><Document /></el-icon>
              登录日志
            </el-button>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>系统信息</span>
          </template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="系统名称">火柴后台管理系统</el-descriptions-item>
            <el-descriptions-item label="系统版本">1.0.0</el-descriptions-item>
            <el-descriptions-item label="技术栈">Spring Boot 3.5 + Vue 3 + Element Plus</el-descriptions-item>
            <el-descriptions-item label="认证方式">JWT + Redis</el-descriptions-item>
            <el-descriptions-item label="权限框架">RBAC</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 上次登录时间
const lastLoginTime = ref(new Date().toLocaleString())

// 统计卡片数据
const statsCards = ref([
  { key: 'users', label: '用户总数', value: '128', icon: 'User', color: '#409EFF' },
  { key: 'roles', label: '角色总数', value: '5', icon: 'UserFilled', color: '#67C23A' },
  { key: 'permissions', label: '权限总数', value: '32', icon: 'Lock', color: '#E6A23C' },
  { key: 'logs', label: '今日登录', value: '56', icon: 'Document', color: '#F56C6C' }
])
</script>

<style scoped lang="scss">
.dashboard-container {
  .welcome-card {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #fff;
    margin-bottom: 20px;
    
    .welcome-content {
      h2 {
        margin: 0 0 10px 0;
        font-size: 24px;
      }
      
      p {
        margin: 0;
        opacity: 0.8;
      }
    }
  }
  
  .stats-card {
    .stats-content {
      display: flex;
      align-items: center;
      gap: 15px;
      
      .stats-icon {
        width: 60px;
        height: 60px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
      }
      
      .stats-info {
        .stats-value {
          font-size: 28px;
          font-weight: bold;
          color: #333;
        }
        
        .stats-label {
          color: #999;
          font-size: 14px;
        }
      }
    }
  }
  
  .quick-actions {
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }
}
</style>
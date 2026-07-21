<template>
  <router-view />
  <!-- 安卓/Chrome 可安装时,显示安装到桌面按钮 -->
  <div v-if="canInstall" class="pwa-install" @click="install">
    📲 安装到桌面
    <span class="pwa-close" @click.stop="canInstall = false">✕</span>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const canInstall = ref(false)
let deferred = null

onMounted(() => {
  window.addEventListener('beforeinstallprompt', (e) => {
    e.preventDefault()
    deferred = e
    canInstall.value = true
  })
  window.addEventListener('appinstalled', () => { canInstall.value = false })
})

async function install() {
  if (!deferred) { canInstall.value = false; return }
  deferred.prompt()
  await deferred.userChoice
  deferred = null
  canInstall.value = false
}
</script>

<style>
.pwa-install {
  position: fixed; right: 16px; bottom: 76px; z-index: 999;
  background: linear-gradient(135deg, #a4232a, #7a1519); color: #fdf6e8;
  padding: 10px 16px; border-radius: 999px; font-size: 14px; font-weight: 600;
  box-shadow: 0 8px 20px rgba(122,21,25,.4); cursor: pointer;
  display: flex; align-items: center; gap: 8px;
}
.pwa-install .pwa-close { opacity: .8; font-size: 12px; padding-left: 4px; }
</style>

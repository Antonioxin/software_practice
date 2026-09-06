<script setup lang="ts">
import type { AccountStatus } from '../types'
defineProps<{ status: AccountStatus }>()
</script>

<template>
  <span :class="['status-badge', status.toLowerCase()]">
    <i aria-hidden="true"></i>{{ status === 'ACTIVE' ? '已启用' : '已停用' }}
  </span>
</template>

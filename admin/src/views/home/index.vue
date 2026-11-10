<script setup lang="ts">
import { computed } from 'vue';
import { $t } from '@/locales';
import { useAuthStore } from '@/store/modules/auth';
import { useAuth } from '@/hooks/business/auth';

defineOptions({
  name: 'Home'
});

const authStore = useAuthStore();
const { hasAuth } = useAuth();

/** Entry points, each shown only when the account could actually open it. */
const shortcuts = computed(() => {
  const items: {
    key: 'system_user' | 'system_role' | 'system_permission';
    label: string;
    icon: string;
    permission: string;
  }[] = [
    {
      key: 'system_user',
      label: $t('route.system_user'),
      icon: 'ic:round-manage-accounts',
      permission: 'user:read'
    },
    {
      key: 'system_role',
      label: $t('route.system_role'),
      icon: 'ic:round-admin-panel-settings',
      permission: 'role:read'
    },
    {
      key: 'system_permission',
      label: $t('route.system_permission'),
      icon: 'ic:round-key',
      permission: 'permission:read'
    }
  ];

  return items.filter(item => hasAuth(item.permission));
});
</script>

<template>
  <NSpace vertical :size="16">
    <NCard :bordered="false" class="card-wrapper">
      <NSpace vertical :size="8">
        <h2 class="text-22px font-500">{{ $t('page.home.greeting', { userName: authStore.userInfo.userName }) }}</h2>
        <p class="text-14px text-#666">{{ $t('page.home.desc') }}</p>
      </NSpace>
    </NCard>
    <NGrid :x-gap="16" :y-gap="16" responsive="screen" item-responsive>
      <NGi v-for="item in shortcuts" :key="item.key" span="24 s:12 m:8">
        <NCard :bordered="false" class="card-wrapper cursor-pointer" @click="$router.push({ name: item.key })">
          <div class="flex-y-center gap-12px">
            <SvgIcon :icon="item.icon" class="text-28px text-primary" />
            <span class="text-16px font-500">{{ item.label }}</span>
          </div>
        </NCard>
      </NGi>
    </NGrid>
  </NSpace>
</template>

<style scoped></style>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { fetchGetPermissionList } from '@/service/api';
import { $t } from '@/locales';

defineOptions({
  name: 'SystemPermission'
});

const loading = ref(false);
const permissions = ref<Api.Permission.Permission[]>([]);

async function loadPermissions() {
  loading.value = true;

  const { data, error } = await fetchGetPermissionList();

  loading.value = false;

  if (!error) {
    permissions.value = data;
  }
}

onMounted(loadPermissions);

const columns = [
  { key: 'code', title: $t('page.systemPermission.code'), width: 200 },
  { key: 'name', title: $t('page.systemPermission.name'), width: 160 },
  { key: 'description', title: $t('page.systemPermission.description') }
];
</script>

<template>
  <div class="flex-col-stretch gap-16px">
    <NAlert type="info" :bordered="false">
      {{ $t('page.systemPermission.readOnlyTip') }}
    </NAlert>
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NDataTable
        :columns="columns"
        :data="permissions"
        :loading="loading"
        :row-key="row => row.id"
        :pagination="{ pageSize: 20, showSizePicker: false }"
      />
    </NCard>
  </div>
</template>

<style scoped></style>

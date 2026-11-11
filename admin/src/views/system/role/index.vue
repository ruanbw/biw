<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import type { Ref } from 'vue';
import { NButton, NTag } from 'naive-ui';
import type { FormInst, FormRules, SelectOption } from 'naive-ui';
import dayjs from 'dayjs';
import type { FlatResponseData } from '@sa/axios';
import {
  fetchAssignRolePermissions,
  fetchCreateRole,
  fetchDeleteRole,
  fetchGetPermissionList,
  fetchGetRole,
  fetchGetRoleList,
  fetchUpdateRole
} from '@/service/api';
import { $t } from '@/locales';
import { useAuth } from '@/hooks/business/auth';
import { transformPage } from '@/hooks/business/pagination';
import type { PageEnvelope } from '@/hooks/business/pagination';
import { useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';

defineOptions({
  name: 'SystemRole'
});

type Role = Api.Role.Role;

const { hasAuth } = useAuth();

const canWrite = computed(() => hasAuth('role:write'));

/**
 * Query state the request reads.
 *
 * The table hook owns its own pagination object, so the page/size pair it emits is mirrored
 * here before each fetch; reading that object from inside `api` would be circular.
 */
const query = reactive({ page: 1, size: 10, keyword: '' });

/** Inside `computed` so the labels follow the active locale; a plain array would freeze them. */
const statusOptions = computed<SelectOption[]>(() => [
  { label: $t('page.systemRole.enabled'), value: 'ENABLED' },
  { label: $t('page.systemRole.disabled'), value: 'DISABLED' }
]);

const roleColumns = computed<NaiveUI.TableColumn<Role>[]>(() => [
  { key: 'code', title: $t('page.systemRole.code'), width: 160 },
  { key: 'name', title: $t('page.systemRole.name'), width: 160 },
  { key: 'description', title: $t('page.systemRole.description'), minWidth: 200 },
  {
    key: 'status',
    title: $t('page.systemRole.status'),
    width: 100,
    render: row =>
      h(NTag, { size: 'small', type: row.status === 'ENABLED' ? 'success' : 'default', bordered: false }, () =>
        row.status === 'ENABLED' ? $t('page.systemRole.enabled') : $t('page.systemRole.disabled')
      )
  },
  {
    key: 'builtIn',
    title: $t('page.systemRole.builtIn'),
    width: 100,
    render: row =>
      h(NTag, { size: 'small', type: row.builtIn ? 'warning' : 'default', bordered: false }, () =>
        row.builtIn ? $t('common.yesOrNo.yes') : $t('common.yesOrNo.no')
      )
  },
  {
    key: 'createdAt',
    title: $t('page.systemRole.createdAt'),
    width: 180,
    render: row => dayjs(row.createdAt).format('YYYY-MM-DD HH:mm')
  },
  {
    key: 'operate',
    title: $t('common.operate'),
    width: 240,
    align: 'center',
    fixed: 'right',
    render: row =>
      h('div', { class: 'flex-center gap-8px' }, [
        // `manageable` is computed by the backend with the same rule its write path enforces:
        // role:write alone is not enough when the row carries permissions the caller lacks.
        canWrite.value && row.manageable !== false
          ? h(NButton, { size: 'tiny', quaternary: true, type: 'primary', onClick: () => openEdit(row.id) }, () =>
              $t('common.edit')
            )
          : null,
        canWrite.value && row.permissionsEditable !== false
          ? h(NButton, { size: 'tiny', quaternary: true, onClick: () => openPermissions(row.id) }, () =>
              $t('page.systemRole.permissionTitle')
            )
          : null,
        // Built-in roles can never be deleted, so the button would only earn a 403.
        canWrite.value && row.manageable !== false && !row.builtIn
          ? h(NButton, { size: 'tiny', quaternary: true, type: 'error', onClick: () => handleDelete(row.id) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

const { columns, columnChecks, data, loading, pagination, getData, mobilePagination } = useNaivePaginatedTable({
  api: () =>
    fetchGetRoleList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    }),
  transform: (response: FlatResponseData<App.Service.Response<any>, PageEnvelope<Role>>) =>
    transformPage<Role>(response),
  columns: () => roleColumns.value,
  paginationProps: { pageSizes: [10, 20, 50, 100] },
  onPaginationParamsChange: ({ page, pageSize }) => {
    query.page = page ?? 1;
    query.size = pageSize ?? 10;
  }
});

const { drawerVisible, closeDrawer, operateType, handleAdd, editingData, handleEdit, onDeleted } =
  useTableOperate(data as Ref<Role[]>, 'id', getData, pagination);

function search() {
  // A changed page triggers the hook's watch, which refreshes for us.
  if (pagination.page !== 1) {
    pagination.page = 1;
  } else {
    getData();
  }
}

function resetSearch() {
  query.keyword = '';
  search();
}

// ---------------------------------------------------------------
// Create / edit
// ---------------------------------------------------------------
interface FormModel {
  code: string;
  name: string;
  description: string;
  status: Api.Role.Status;
}

const formModel = reactive<FormModel>({
  code: '',
  name: '',
  description: '',
  status: 'ENABLED'
});

function openCreate() {
  formModel.code = '';
  formModel.name = '';
  formModel.description = '';
  formModel.status = 'ENABLED';
  handleAdd();
}

function openEdit(id: number) {
  handleEdit(id);
}

/** `handleEdit` clones the row into `editingData`; mirror it into the form once it is set. */
watch(editingData, value => {
  if (operateType.value !== 'edit' || !value) return;

  formModel.code = value.code;
  formModel.name = value.name;
  formModel.description = value.description;
  formModel.status = value.status;
});

const saving = ref(false);
const formRef = ref<FormInst | null>(null);

/** Mirrors the backend `CreateRoleRequest`/`UpdateRoleRequest` constraints. */
const ROLE_CODE_PATTERN = /^[A-Z][A-Z0-9_]{0,31}$/;
const MAX_ROLE_NAME_LENGTH = 64;
const MAX_ROLE_DESCRIPTION_LENGTH = 255;

/**
 * `code` is immutable once the role exists (the input is disabled in edit mode), so it is only
 * validated on create: a disabled field would otherwise block a form it cannot fix.
 */
const rules = computed<FormRules>(() => ({
  code:
    operateType.value === 'add'
      ? [
          { required: true, message: $t('page.systemRole.codeRequired'), trigger: ['input', 'blur'] },
          { pattern: ROLE_CODE_PATTERN, message: $t('page.systemRole.codeInvalid'), trigger: ['input', 'blur'] }
        ]
      : [],
  name: [
    { required: true, message: $t('page.systemRole.nameRequired'), trigger: ['input', 'blur'] },
    { max: MAX_ROLE_NAME_LENGTH, message: $t('page.systemRole.nameTooLong'), trigger: ['input', 'blur'] }
  ],
  description: [
    { max: MAX_ROLE_DESCRIPTION_LENGTH, message: $t('page.systemRole.descriptionTooLong'), trigger: ['input', 'blur'] }
  ]
}));

async function submitForm() {
  const valid = await formRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false);

  if (!valid) {
    return;
  }

  saving.value = true;

  const result =
    operateType.value === 'add'
      ? await fetchCreateRole({
          code: formModel.code,
          name: formModel.name,
          description: formModel.description
        })
      : await fetchUpdateRole(Number(editingData.value?.id), {
          name: formModel.name,
          description: formModel.description,
          status: formModel.status
        });

  saving.value = false;

  if (!result.error) {
    window.$message?.success(operateType.value === 'add' ? $t('common.addSuccess') : $t('common.updateSuccess'));
    closeDrawer();
    await getData();
  }
}

function handleDelete(id: number) {
  window.$dialog?.warning({
    title: $t('common.tip'),
    content: $t('common.confirmDelete'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      const { error } = await fetchDeleteRole(id);

      if (!error) {
        await onDeleted();
      }
    }
  });
}

// ---------------------------------------------------------------
// Assign permissions
// ---------------------------------------------------------------
const permissionVisible = ref(false);
const permissionTargetId = ref(0);
const permissionTargetBuiltIn = ref(false);
const permissionCodes = ref<string[]>([]);
const allPermissions = ref<Api.Permission.Permission[]>([]);
const permissionLoading = ref(false);

const permissionOptions = computed<SelectOption[]>(() =>
  allPermissions.value.map(item => ({
    label: `${item.name} (${item.code})`,
    value: item.code
  }))
);

/**
 * The sequence number of the newest `openPermissions` call.
 *
 * Two clicks in quick succession leave two requests in flight; without a marker the slower
 * answer of the first row would overwrite the dialog with the wrong role's permission set.
 */
let permissionRequestSeq = 0;

/**
 * Load the role's current permissions first and only then open the dialog.
 *
 * Opening it up front would leave an empty selection on screen when the request fails, and
 * "confirm" would submit `permissionCodes: []` — which the API treats as "revoke everything".
 */
async function openPermissions(id: number) {
  const seq = ++permissionRequestSeq;

  permissionCodes.value = [];
  // Reset before the request so a failure cannot inherit the previous role's state.
  permissionTargetBuiltIn.value = false;
  permissionLoading.value = true;

  const [
    { data: detail, error },
    { data: catalog, error: catalogError }
  ] = await Promise.all([
    fetchGetRole(id),
    allPermissions.value.length
      ? Promise.resolve({ data: allPermissions.value, error: null })
      : fetchGetPermissionList()
  ]);

  // A newer click owns the dialog now; this answer is stale and must not touch any state.
  if (seq !== permissionRequestSeq) return;

  permissionLoading.value = false;

  if (error || !detail) return;

  if (catalogError && !allPermissions.value.length) {
    // The picker would open with no options at all, which looks like "this role has no
    // permissions" — the one state that invites wiping the role. Say it failed instead.
    window.$message?.error($t('page.systemRole.permissionCatalogFailed'));
    return;
  }

  // Only claim the target once the answer proved to belong to it.
  permissionTargetId.value = id;
  permissionTargetBuiltIn.value = detail.role.builtIn;
  // ADMIN answers the wildcard, which is not a selectable option; showing it would let the
  // form submit a code the catalog does not contain.
  permissionCodes.value = detail.permissionCodes.filter(code => code !== '*');

  if (catalog) {
    allPermissions.value = catalog;
  }

  permissionVisible.value = true;
}

async function submitPermissions() {
  const { error } = await fetchAssignRolePermissions(permissionTargetId.value, {
    permissionCodes: permissionCodes.value
  });

  if (!error) {
    window.$message?.success($t('page.systemRole.assignSuccess'));
    permissionVisible.value = false;
  }
}
</script>

<template>
  <div class="flex-col-stretch gap-16px">
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NSpace wrap justify="space-between" align="center">
        <NSpace wrap align="center">
          <NInput
            v-model:value="query.keyword"
            clearable
            :placeholder="$t('page.systemRole.keywordPlaceholder')"
            class="w-240px"
            @keyup.enter="search"
          />
          <NButton type="primary" @click="search">{{ $t('common.search') }}</NButton>
          <NButton @click="resetSearch">{{ $t('common.reset') }}</NButton>
        </NSpace>
        <!-- 后端无角色批量删除接口：禁用"批量删除"按钮，不新增接口调用。 -->
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="true"
          :loading="loading"
          @add="openCreate"
          @refresh="getData"
        >
          <template v-if="!canWrite" #default>
            <span />
          </template>
        </TableHeaderOperation>
      </NSpace>
    </NCard>

    <NCard :bordered="false" size="small" class="card-wrapper sm:flex-1-hidden">
      <NDataTable
        :columns="columns"
        :data="data"
        :loading="loading"
        :row-key="row => row.id"
        :pagination="mobilePagination"
        :scroll-x="1000"
        class="sm:h-full"
      />
    </NCard>

    <NDrawer v-model:show="drawerVisible" :width="480" placement="right">
      <NDrawerContent
        :title="operateType === 'add' ? $t('page.systemRole.createTitle') : $t('page.systemRole.editTitle')"
        closable
      >
        <NForm ref="formRef" label-placement="top" :model="formModel" :rules="rules" @submit.prevent="submitForm">
          <NFormItem path="code" :label="$t('page.systemRole.code')">
            <NInput
              v-model:value="formModel.code"
              :disabled="operateType === 'edit'"
              placeholder="AUDITOR"
              :maxlength="32"
            />
          </NFormItem>
          <p v-if="operateType === 'add'" class="mb-16px text-12px text-#888">{{ $t('page.systemRole.codeTip') }}</p>
          <NFormItem path="name" :label="$t('page.systemRole.name')">
            <NInput v-model:value="formModel.name" :maxlength="64" />
          </NFormItem>
          <NFormItem path="description" :label="$t('page.systemRole.description')">
            <NInput v-model:value="formModel.description" type="textarea" :maxlength="255" :rows="3" />
          </NFormItem>
          <NFormItem v-if="operateType === 'edit'" :label="$t('page.systemRole.status')">
            <NSelect
              v-model:value="formModel.status"
              :options="statusOptions"
              :disabled="Boolean(editingData?.builtIn)"
            />
          </NFormItem>
          <p v-if="editingData?.builtIn" class="text-12px text-#888">{{ $t('page.systemRole.builtInRoleTip') }}</p>
        </NForm>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
            <NButton type="primary" :loading="saving" @click="submitForm">{{ $t('common.confirm') }}</NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <NModal
      v-model:show="permissionVisible"
      preset="card"
      class="w-560px"
      :title="$t('page.systemRole.permissionTitle')"
    >
      <NAlert v-if="permissionTargetBuiltIn" type="warning" :bordered="false" class="mb-16px">
        {{ $t('page.systemRole.permissionBuiltInTip') }}
      </NAlert>
      <p v-else class="mb-16px text-13px text-#888">{{ $t('page.systemRole.permissionTip') }}</p>
      <NSelect
        v-model:value="permissionCodes"
        multiple
        filterable
        :options="permissionOptions"
        :loading="permissionLoading"
        :disabled="permissionTargetBuiltIn"
        :placeholder="$t('page.systemRole.permissionEmpty')"
      />
      <template #footer>
        <NSpace justify="end">
          <NButton @click="permissionVisible = false">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" :disabled="permissionTargetBuiltIn || permissionLoading" @click="submitPermissions">
            {{ $t('common.confirm') }}
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped></style>

<script setup lang="ts">
import { computed, h, reactive, ref, watch } from 'vue';
import type { Ref } from 'vue';
import { NButton, NTag } from 'naive-ui';
import type { FormInst, FormItemRule, FormRules, SelectOption } from 'naive-ui';
import { useDebounceFn } from '@vueuse/core';
import dayjs from 'dayjs';
import type { FlatResponseData } from '@sa/axios';
import {
  fetchBatchDeleteUsers,
  fetchCreateUser,
  fetchDeleteUser,
  fetchGetRoleOptions,
  fetchGetUserList,
  fetchResetUserPassword,
  fetchUpdateUser
} from '@/service/api';
import { $t } from '@/locales';
import { useAuth } from '@/hooks/business/auth';
import { transformPage } from '@/hooks/business/pagination';
import type { PageEnvelope } from '@/hooks/business/pagination';
import { useNaivePaginatedTable, useTableOperate } from '@/hooks/common/table';
import { useAuthStore } from '@/store/modules/auth';

defineOptions({
  name: 'SystemUser'
});

type User = Api.User.User;

const { hasAuth } = useAuth();

const authStore = useAuthStore();

const canWrite = computed(() => hasAuth('user:write'));

/**
 * Query state the request reads.
 *
 * The table hook owns its own pagination object, so the page/size pair it emits is mirrored
 * here before each fetch; reading `pagination` from inside `api` would be circular.
 */
const query = reactive({
  page: 1,
  size: 10,
  keyword: ''
});

/** Inside `computed` so the labels follow the active locale; a plain array would freeze them. */
const statusOptions = computed<SelectOption[]>(() => [
  { label: $t('page.systemUser.enabled'), value: 'ENABLED' },
  { label: $t('page.systemUser.disabled'), value: 'DISABLED' }
]);

const userColumns = computed<NaiveUI.TableColumn<User>[]>(() => [
  // Selection only exists to feed the batch delete the caller may not have.
  ...(canWrite.value ? [{ type: 'selection', width: 48 } as NaiveUI.TableColumn<User>] : []),
  { key: 'username', title: $t('page.systemUser.username'), width: 160 },
  { key: 'nickname', title: $t('page.systemUser.nickname'), width: 160 },
  {
    key: 'roleCodes',
    title: $t('page.systemUser.roles'),
    minWidth: 180,
    render: row =>
      h(
        'div',
        { class: 'flex flex-wrap gap-4px' },
        row.roleCodes?.length
          ? row.roleCodes.map(code => h(NTag, { size: 'small', type: 'info', bordered: false }, () => code))
          : $t('page.systemUser.noRole')
      )
  },
  {
    key: 'status',
    title: $t('page.systemUser.status'),
    width: 100,
    render: row =>
      h(NTag, { size: 'small', type: row.status === 'ENABLED' ? 'success' : 'default', bordered: false }, () =>
        row.status === 'ENABLED' ? $t('page.systemUser.enabled') : $t('page.systemUser.disabled')
      )
  },
  {
    key: 'createdAt',
    title: $t('page.systemUser.createdAt'),
    width: 180,
    render: row => dayjs(row.createdAt).format('YYYY-MM-DD HH:mm')
  },
  {
    key: 'operate',
    title: $t('common.operate'),
    width: 230,
    align: 'center',
    fixed: 'right',
    render: row =>
      h('div', { class: 'flex-center gap-8px' }, [
        // `manageable` is computed by the backend: user:write opens the module, but an
        // administrator or an account holding more permissions still answers false.
        canWrite.value && row.manageable !== false
          ? h(NButton, { size: 'tiny', quaternary: true, type: 'primary', onClick: () => openEdit(row.id) }, () =>
              $t('common.edit')
            )
          : null,
        canWrite.value && row.manageable !== false
          ? h(NButton, { size: 'tiny', quaternary: true, onClick: () => openResetPassword(row.id) }, () =>
              $t('page.systemUser.resetPasswordTitle')
            )
          : null,
        canWrite.value && row.manageable !== false
          ? h(NButton, { size: 'tiny', quaternary: true, type: 'error', onClick: () => confirmDelete(row.id) }, () =>
              $t('common.delete')
            )
          : null
      ])
  }
]);

const { columns, columnChecks, data, loading, pagination, getData, mobilePagination } = useNaivePaginatedTable({
  api: () =>
    fetchGetUserList({
      page: query.page,
      size: query.size,
      keyword: query.keyword || undefined
    }),
  transform: (response: FlatResponseData<App.Service.Response<any>, PageEnvelope<User>>) =>
    transformPage<User>(response),
  columns: () => userColumns.value,
  paginationProps: { pageSizes: [10, 20, 50, 100] },
  onPaginationParamsChange: ({ page, pageSize }) => {
    query.page = page ?? 1;
    query.size = pageSize ?? 10;
  }
});

const {
  drawerVisible,
  closeDrawer,
  operateType,
  handleAdd,
  editingData,
  handleEdit,
  checkedRowKeys,
  onBatchDeleted,
  onDeleted
} = useTableOperate(data as Ref<User[]>, 'id', getData, pagination);

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
  username: string;
  password: string;
  nickname: string;
  roleCodes: string[];
  status: Api.User.Status;
}

const formModel = reactive<FormModel>({
  username: '',
  password: '',
  nickname: '',
  roleCodes: [],
  status: 'ENABLED'
});

const roleOptions = ref<SelectOption[]>([]);
const roleLoading = ref(false);

/**
 * The sequence number of the newest request that writes the role picker.
 *
 * Both a typed search and the edit-mode lookup fill it, so one shared counter is what keeps a
 * slow answer for "ad" from replacing the options for "admin", or a search from overwriting the
 * resolved names of the row being edited.
 */
let roleOptionsSeq = 0;

/** Turn an `OptionVO` into a picker entry: the label is the role name, the value its code. */
function toRoleOption(item: Api.Role.Option): SelectOption {
  return {
    label: `${item.label} (${item.description})`,
    value: item.description,
    disabled: item.disabled
  };
}

/**
 * Load role pickers.
 *
 * The role option answers the role id as `value` and the role code as `description`, and it is
 * the code the user endpoints submit, so `value` here is the description.
 */
async function loadRoleOptions(keyword?: string) {
  const seq = ++roleOptionsSeq;

  roleLoading.value = true;

  try {
    const { data: options, error } = await fetchGetRoleOptions({ keyword: keyword || undefined, limit: 50 });

    // A newer request owns the picker now.
    if (seq !== roleOptionsSeq) return;

    if (error) {
      // Swallowing this leaves an empty picker with no explanation, which reads as "no roles
      // exist" instead of "the request failed".
      window.$message?.error(error.message);
      return;
    }

    roleOptions.value = options.map(toRoleOption);
  } finally {
    if (seq === roleOptionsSeq) {
      roleLoading.value = false;
    }
  }
}

/** `NSelect` fires `search` on every keystroke; debounce before spending a request per key. */
const searchRoleOptions = useDebounceFn((keyword: string) => loadRoleOptions(keyword), 300);

/**
 * Resolve the role codes of the row being edited into options that carry the role *name*.
 *
 * The row only holds codes, so the enabled catalogue is used as a code → id index and the ids
 * are then fetched back, which is what `OptionVO.description` needs. Any code the lookup cannot
 * resolve keeps the bare code as its label — a stale name is worse than a visible code.
 */
async function loadRoleOptionsByCodes(codes: string[]) {
  const seq = ++roleOptionsSeq;

  roleLoading.value = true;
  // Show the codes immediately; the names replace them once the lookup answers.
  roleOptions.value = codes.map(code => ({ label: code, value: code }));

  try {
    if (!codes.length) return;

    const { data: catalogue, error: catalogueError } = await fetchGetRoleOptions({ limit: 50 });

    if (seq !== roleOptionsSeq || catalogueError || !catalogue) return;

    const ids = catalogue.filter(item => codes.includes(item.description)).map(item => item.value);

    if (!ids.length) return;

    const { data: selected, error: selectedError } = await fetchGetRoleOptions({ ids });

    if (seq !== roleOptionsSeq || selectedError || !selected) return;

    const labelByCode = new Map(selected.map(item => [item.description, item.label]));

    roleOptions.value = codes.map(code => ({
      label: labelByCode.get(code) ?? code,
      value: code
    }));
  } finally {
    if (seq === roleOptionsSeq) {
      roleLoading.value = false;
    }
  }
}

function openCreate() {
  formModel.username = '';
  formModel.password = '';
  formModel.nickname = '';
  formModel.roleCodes = ['USER'];
  formModel.status = 'ENABLED';
  roleOptions.value = [];
  handleAdd();
  loadRoleOptions();
}

function openEdit(id: number) {
  handleEdit(id);
}

/** `handleEdit` clones the row into `editingData`; mirror it into the form once it is set. */
watch(editingData, value => {
  if (operateType.value !== 'edit' || !value) return;

  formModel.username = value.username;
  formModel.nickname = value.nickname;
  formModel.roleCodes = [...(value.roleCodes ?? [])];
  formModel.status = value.status;
  formModel.password = '';

  loadRoleOptionsByCodes(formModel.roleCodes);
});

// ---------------------------------------------------------------
// Validation
// ---------------------------------------------------------------
/**
 * Mirrors the server rules so a malformed value is refused before the request is sent, and the
 * error points at the field instead of arriving as an English 400 from the API.
 *
 * The password ceiling is in BYTES, not characters: BCrypt reads 72 bytes and the API rejects
 * anything longer, so 30 Chinese characters (90 bytes) must fail here rather than at the server.
 */
const USERNAME_PATTERN = /^[A-Za-z0-9][A-Za-z0-9_.-]{2,31}$/;
const MIN_PASSWORD_CHARS = 8;
const MAX_PASSWORD_CHARS = 64;
const MAX_PASSWORD_BYTES = 72;

const formRef = ref<FormInst | null>(null);
const resetFormRef = ref<FormInst | null>(null);

function passwordRules(): FormItemRule[] {
  return [
    {
      required: true,
      validator(_rule, value: string) {
        if (!value) {
          return new Error($t('page.systemUser.passwordRequired'));
        }
        if (value.length < MIN_PASSWORD_CHARS) {
          return new Error($t('page.systemUser.passwordTooShort'));
        }
        if (value.length > MAX_PASSWORD_CHARS) {
          return new Error($t('page.systemUser.passwordTooLong'));
        }
        if (new TextEncoder().encode(value).length > MAX_PASSWORD_BYTES) {
          return new Error($t('page.systemUser.passwordTooManyBytes'));
        }
        return true;
      },
      trigger: ['input', 'blur']
    }
  ];
}

const rules = computed<FormRules>(() => ({
  username: [
    { required: true, message: $t('page.systemUser.usernameRequired'), trigger: ['input', 'blur'] },
    { pattern: USERNAME_PATTERN, message: $t('page.systemUser.usernameInvalid'), trigger: ['input', 'blur'] }
  ],
  password: operateType.value === 'add' ? passwordRules() : []
}));

const resetRules: FormRules = {
  newPassword: passwordRules()
};

const saving = ref(false);

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
      ? await fetchCreateUser({
          username: formModel.username,
          password: formModel.password,
          nickname: formModel.nickname,
          roleCodes: formModel.roleCodes,
          status: formModel.status
        })
      : await fetchUpdateUser(Number(editingData.value?.id), {
          nickname: formModel.nickname,
          roleCodes: formModel.roleCodes,
          status: formModel.status
        });

  saving.value = false;

  if (!result.error) {
    window.$message?.success(operateType.value === 'add' ? $t('common.addSuccess') : $t('common.updateSuccess'));
    closeDrawer();

    if (operateType.value === 'add') {
      // The list is newest first, so a new user is only visible on the first page. Changing the
      // page fires the table's own watcher, which refetches; on page 1 there is nothing to
      // change, so refresh directly.
      if (pagination.page !== 1) {
        pagination.page = 1;
        return;
      }
    }

    await getData();
  }
}

// ---------------------------------------------------------------
// Delete
// ---------------------------------------------------------------
function confirmDelete(id: number) {
  window.$dialog?.warning({
    title: $t('common.tip'),
    content: $t('common.confirmDelete'),
    positiveText: $t('common.confirm'),
    negativeText: $t('common.cancel'),
    onPositiveClick: async () => {
      const { error } = await fetchDeleteUser(id);

      if (!error) {
        await onDeleted();
      }
    }
  });
}

/**
 * Batch delete.
 *
 * The "batch delete" button in `TableHeaderOperation` already asks for confirmation through an
 * `NPopconfirm`, so a second dialog here would ask the same question twice.
 */
async function handleBatchDelete() {
  const { error } = await fetchBatchDeleteUsers({ ids: checkedRowKeys.value.map(Number) });

  if (!error) {
    await onBatchDeleted();
  }
}

// ---------------------------------------------------------------
// Reset password
// ---------------------------------------------------------------
const resetVisible = ref(false);
const resetTargetId = ref(0);
/**
 * The reset form's model.
 *
 * `NForm` resolves a field's value with `get(model, path)`, so the form must be given a model
 * whose keys match the `NFormItem` paths: without it `path="newPassword"` reads `undefined` and
 * the required rule can never pass.
 */
const resetModel = reactive({ newPassword: '' });

/** The reset revokes every session of the target, including the caller's own. */
const resetTargetIsSelf = computed(() => String(resetTargetId.value) === authStore.userInfo.userId);

const resetSubmitting = ref(false);

function openResetPassword(id: number) {
  resetTargetId.value = id;
  resetModel.newPassword = '';
  resetVisible.value = true;
}

/** Closing through the modal's own controls leaves the typed password behind; drop it. */
watch(resetVisible, visible => {
  if (!visible) {
    resetModel.newPassword = '';
  }
});

async function submitResetPassword() {
  // A double click would send the same reset twice and revoke the sessions twice.
  if (resetSubmitting.value) return;

  const valid = await resetFormRef.value
    ?.validate()
    .then(() => true)
    .catch(() => false);

  if (!valid) {
    return;
  }

  resetSubmitting.value = true;

  try {
    const { error } = await fetchResetUserPassword(resetTargetId.value, { newPassword: resetModel.newPassword });

    if (!error) {
      window.$message?.success($t('common.modifySuccess'));
      resetVisible.value = false;
    }
  } finally {
    resetSubmitting.value = false;
  }
}

const drawerTitle = computed(() =>
  operateType.value === 'add' ? $t('page.systemUser.createTitle') : $t('page.systemUser.editTitle')
);
</script>

<template>
  <div class="flex-col-stretch gap-16px">
    <NCard :bordered="false" size="small" class="card-wrapper">
      <NSpace wrap justify="space-between" align="center">
        <NSpace wrap align="center">
          <NInput
            v-model:value="query.keyword"
            clearable
            :placeholder="$t('page.systemUser.keywordPlaceholder')"
            class="w-240px"
            @keyup.enter="search"
          />
          <NButton type="primary" @click="search">{{ $t('common.search') }}</NButton>
          <NButton @click="resetSearch">{{ $t('common.reset') }}</NButton>
        </NSpace>
        <TableHeaderOperation
          v-model:columns="columnChecks"
          :disabled-delete="checkedRowKeys.length === 0"
          :loading="loading"
          @add="openCreate"
          @delete="handleBatchDelete"
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
        :checked-row-keys="checkedRowKeys"
        :scroll-x="1000"
        class="sm:h-full"
        @update:checked-row-keys="keys => (checkedRowKeys = keys as string[])"
      />
    </NCard>

    <NDrawer v-model:show="drawerVisible" :width="480" placement="right">
      <NDrawerContent :title="drawerTitle" closable>
        <NForm ref="formRef" label-placement="top" :model="formModel" :rules="rules">
          <NFormItem path="username" :label="$t('page.systemUser.username')">
            <NInput
              v-model:value="formModel.username"
              :disabled="operateType === 'edit'"
              :maxlength="32"
              :placeholder="$t('page.systemUser.username')"
            />
          </NFormItem>
          <NFormItem v-if="operateType === 'add'" path="password" :label="$t('page.systemUser.password')">
            <NInput
              v-model:value="formModel.password"
              type="password"
              show-password-on="click"
              :maxlength="64"
            />
          </NFormItem>
          <NFormItem :label="$t('page.systemUser.nickname')">
            <NInput v-model:value="formModel.nickname" :maxlength="64" />
          </NFormItem>
          <NFormItem :label="$t('page.systemUser.roles')">
            <NSelect
              v-model:value="formModel.roleCodes"
              multiple
              filterable
              remote
              clearable
              :options="roleOptions"
              :loading="roleLoading"
              @search="searchRoleOptions"
            />
          </NFormItem>
          <NFormItem :label="$t('page.systemUser.status')">
            <NSelect v-model:value="formModel.status" :options="statusOptions" />
          </NFormItem>
        </NForm>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="closeDrawer">{{ $t('common.cancel') }}</NButton>
            <NButton type="primary" :loading="saving" @click="submitForm">{{ $t('common.confirm') }}</NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <NModal v-model:show="resetVisible" preset="card" class="w-400px" :title="$t('page.systemUser.resetPasswordTitle')">
      <NAlert v-if="resetTargetIsSelf" type="warning" :bordered="false" class="mb-16px">
        {{ $t('page.systemUser.selfResetPasswordTip') }}
      </NAlert>
      <NForm ref="resetFormRef" label-placement="top" :model="resetModel" :rules="resetRules">
        <NFormItem path="newPassword" :label="$t('page.systemUser.newPassword')">
          <NInput v-model:value="resetModel.newPassword" type="password" show-password-on="click" :maxlength="64" />
        </NFormItem>
      </NForm>
      <template #footer>
        <NSpace justify="end">
          <NButton @click="resetVisible = false">{{ $t('common.cancel') }}</NButton>
          <NButton type="primary" :loading="resetSubmitting" :disabled="resetSubmitting" @click="submitResetPassword">
            {{ $t('common.confirm') }}
          </NButton>
        </NSpace>
      </template>
    </NModal>
  </div>
</template>

<style scoped></style>

<script setup lang="ts">
import { computed, reactive } from 'vue';
import { $t } from '@/locales';
import { useAuthStore } from '@/store/modules/auth';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';

defineOptions({
  name: 'PwdLogin'
});

const authStore = useAuthStore();
const { formRef, validate } = useNaiveForm();

interface FormModel {
  userName: string;
  password: string;
}

const model: FormModel = reactive({
  userName: '',
  password: ''
});

const rules = computed<Record<keyof FormModel, App.Global.FormRule[]>>(() => {
  // inside computed to make locale reactive, if not apply i18n, you can define it without computed
  const { createRequiredRule } = useFormRules();

  /**
   * Login only checks that both fields are filled in.
   *
   * The account creation rules (username charset, password length) belong to the user form: an
   * existing password that predates them would be refused here, locking the account out of a
   * form it cannot fix. The backend is the one who decides whether the pair is valid.
   */
  return {
    userName: [createRequiredRule($t('form.userName.required'))],
    password: [createRequiredRule($t('form.pwd.required'))]
  };
});

async function handleSubmit() {
  // Enter on the form and a click on the button both land here; a second submission while the
  // first is in flight would fire a parallel login request.
  if (authStore.loginLoading) {
    return;
  }

  try {
    await validate();
  } catch {
    // naive-ui rejects with the failed rules; that is not an error to propagate.
    return;
  }

  await authStore.login(model.userName, model.password);
}
</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NFormItem path="userName">
      <NInput v-model:value="model.userName" :placeholder="$t('page.login.common.userNamePlaceholder')" />
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      />
    </NFormItem>
    <NSpace vertical :size="24">
      <NButton type="primary" size="large" round block :loading="authStore.loginLoading" @click="handleSubmit">
        {{ $t('common.confirm') }}
      </NButton>
      <p class="text-14px text-#666 text-center">
        {{ $t('page.login.pwdLogin.adminTip') }}
      </p>
    </NSpace>
  </NForm>
</template>

<style scoped></style>

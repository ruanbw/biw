<script setup lang="ts">
interface Props {
  url: string;
}

defineProps<Props>();
</script>

<template>
  <div class="h-full">
    <!--
      `sandbox` keeps an embedded third-party page from navigating the top frame, opening
      popups, submitting forms to us or reaching into our DOM. `allow-scripts` and
      `allow-same-origin` are required for apps that need to run and read their own storage;
      `allow-forms` for pages that post back to themselves.
    -->
    <iframe
      id="iframePage"
      class="size-full"
      :src="url"
      sandbox="allow-scripts allow-same-origin allow-forms"
    ></iframe>
  </div>
</template>

<style scoped></style>

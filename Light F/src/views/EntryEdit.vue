<template>
  <div class="entry-edit">
    <el-breadcrumb separator="/" class="crumb">
      <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item :to="{ path: `/module/${moduleId}` }">{{ moduleName }}</el-breadcrumb-item>
      <el-breadcrumb-item>{{ isEdit ? '编辑笔记' : '新建笔记' }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="editor-shell vl-card">
      <header class="editor-header">
        <div><span class="eyebrow">{{ isEdit ? 'EDIT NOTE' : 'NEW NOTE' }}</span><h1>{{ isEdit ? '编辑笔记' : '创建一篇新笔记' }}</h1></div>
        <span class="save-state" :class="{ dirty: isDirty }">{{ saveState }}</span>
      </header>

      <el-form :model="form" ref="formRef" :rules="rules" label-position="top" class="editor-form">
        <el-form-item prop="title" class="title-item"><el-input v-model="form.title" placeholder="输入笔记标题…" maxlength="200" /></el-form-item>
        <div class="meta-row">
          <el-form-item label="标签" class="meta-item meta-tags"><el-input v-model="form.tags" placeholder="SpringBoot, 后端，用逗号分隔" /></el-form-item>
          <el-form-item label="学习状态" class="meta-item"><el-select v-model="form.status" class="meta-select"><el-option label="未做" :value="0" /><el-option label="在做" :value="1" /><el-option label="已掌握" :value="2" /></el-select></el-form-item>
          <el-form-item label="笔记类型" class="meta-item"><el-select v-model="form.type" class="meta-select"><el-option label="普通笔记" value="note" /><el-option label="算法题" value="algorithm" /><el-option label="面试问答" value="qa" /><el-option label="资料链接" value="link" /></el-select></el-form-item>
        </div>
        <div v-if="form.type === 'algorithm'" class="algorithm-row"><el-input v-model="algoForm.leetcodeId" placeholder="LeetCode 题号" /><el-select v-model="algoForm.difficulty" placeholder="难度"><el-option label="简单" value="easy" /><el-option label="中等" value="medium" /><el-option label="困难" value="hard" /></el-select><el-input v-model="algoForm.complexity" placeholder="时间复杂度，如 O(n)" /></div>

        <div class="mode-tabs"><button type="button" :class="{ active: editorMode === 'edit' }" @click="editorMode = 'edit'">编辑</button><button type="button" :class="{ active: editorMode === 'preview' }" @click="editorMode = 'preview'">预览</button><button type="button" :class="{ active: editorMode === 'split' }" @click="editorMode = 'split'">左右分栏</button></div>
        <el-form-item label="正文" class="content-item">
          <div class="editor-stage" :class="`mode-${editorMode}`">
            <MdEditor v-if="editorMode !== 'preview'" v-model="form.contentMd" :toolbars="toolbars" :preview="false" :show-code-row-number="true" :sanitize="replaceCachedFileUrls" class="editor" @on-upload-img="handleEditorUpload" @on-change="markDirty" />
            <div v-if="editorMode !== 'edit'" class="preview-pane md-content" v-html="previewHtml"></div>
          </div>
        </el-form-item>

        <div class="material-bar"><span class="material-hint">支持 Markdown、图片、代码块和表格</span><label class="material-action"><input type="file" multiple @change="handleAttachmentChange" /><span>＋ 添加资料</span></label></div>
        <div v-if="attachments.length" class="attachment-list"><div v-for="file in attachments" :key="file.id" class="attachment-chip"><span class="attachment-icon">{{ isImage(file.contentType) ? '▧' : '▤' }}</span><span class="attachment-name">{{ file.originalName }}</span><span class="attachment-size">{{ formatSize(file.fileSize) }}</span></div></div>
      </el-form>
    </div>

    <div class="save-bar"><div class="save-bar-inner"><span class="save-state">{{ saveState }}</span><div class="save-actions"><el-button @click="goBack" round>取消</el-button><button class="btn-ink" :disabled="saving" @click="handleSubmit">{{ saving ? '保存中…' : isEdit ? '保存修改' : '创建笔记' }}</button></div></div></div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { MdEditor } from 'md-editor-v3'
import type { ToolbarNames, UploadImgCallBack } from 'md-editor-v3'
import MarkdownIt from 'markdown-it'
import 'md-editor-v3/lib/style.css'
import { entryApi, fileApi, moduleApi } from '@/api'
import { primeLocalFile, replaceCachedFileUrls, warmupImagesFromMarkdown } from '@/utils/fileUrl'
import type { Entry, EntryType, FileMetadata } from '@/types'

const route = useRoute(); const router = useRouter(); const moduleId = Number(route.params.moduleId); const entryId = route.params.id ? Number(route.params.id) : null; const isEdit = !!entryId
const md = new MarkdownIt({ html: false, linkify: true, breaks: true })
const toolbars: ToolbarNames[] = ['bold', 'underline', 'italic', '-', 'title', 'strikeThrough', 'quote', 'unorderedList', 'orderedList', 'task', '-', 'codeRow', 'code', 'link', 'image', 'table', '-', 'revoke', 'next', '=', 'pageFullscreen']
const moduleName = ref(''); const formRef = ref<FormInstance>(); const saving = ref(false); const isDirty = ref(false); const lastSavedAt = ref(''); const editorMode = ref<'edit' | 'preview' | 'split'>('edit'); const attachments = ref<FileMetadata[]>([])
const form = reactive<Partial<Entry>>({ moduleId, type: (route.query.type as EntryType) || 'note', title: '', contentMd: '', tags: '', status: 0 }); const algoForm = reactive({ leetcodeId: '', difficulty: '', complexity: '' }); const rules: FormRules = { title: [{ required: true, message: '请输入标题', trigger: 'blur' }] }
const saveState = computed(() => saving.value ? '正在保存…' : isDirty.value ? '有未保存修改' : lastSavedAt.value ? `已保存 ${lastSavedAt.value}` : '草稿未保存')
// 图片 blob 缓存就绪后 +1，驱动预览 HTML 重新经过 sanitize 替换
const imgTick = ref(0)
const previewHtml = computed(() => { imgTick.value; return replaceCachedFileUrls(md.render(form.contentMd || '*开始写下你的笔记吧…*')) })
const markDirty = () => { isDirty.value = true }; const formatSize = (size: number) => size < 1024 * 1024 ? `${Math.max(1, Math.round(size / 1024))} KB` : `${(size / 1024 / 1024).toFixed(1)} MB`; const isImage = (type: string) => type.startsWith('image/')

/* 上传后：正文写入受保护稳定路径（入库），同时用本地 File 预热 blob 缓存，编辑器 sanitize 同步命中 */
const uploadFiles = async (files: File[], callback?: UploadImgCallBack) => { if (!entryId) { ElMessage.warning('请先创建笔记，再添加图片或资料'); return }; const urls: { url: string; alt: string; title: string }[] = []; for (const file of files) { try { const result = await fileApi.upload(file, entryId); const metadata = result.data; if (metadata) { primeLocalFile(metadata.id, file); attachments.value.unshift(metadata); urls.push({ url: fileApi.url(metadata.id), alt: file.name, title: file.name }) } } catch { ElMessage.error(`上传失败：${file.name}`) } } if (callback && urls.length) callback(urls) }
const handleEditorUpload = (files: File[], callback: UploadImgCallBack) => uploadFiles(files, callback)
const handleAttachmentChange = async (event: Event) => { const input = event.target as HTMLInputElement; if (input.files?.length) await uploadFiles(Array.from(input.files)); input.value = '' }

const handleSubmit = async () => { const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; saving.value = true; const payload: Partial<Entry> = { ...form }; if (algoForm.leetcodeId || algoForm.difficulty || algoForm.complexity) payload.dataJson = JSON.stringify(algoForm); try { if (isEdit && entryId) { await entryApi.update(entryId, payload); ElMessage.success('保存成功'); lastSavedAt.value = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); isDirty.value = false; router.push(`/entry/${entryId}`) } else { await entryApi.create(payload); ElMessage.success('创建成功'); router.push(`/module/${moduleId}`) } } finally { saving.value = false } }
const goBack = () => router.back()
onMounted(async () => { const mRes = await moduleApi.detail(moduleId); moduleName.value = mRes.data.name; if (isEdit && entryId) { const res = await entryApi.detail(entryId); Object.assign(form, res.data); if (res.data.dataJson) { try { Object.assign(algoForm, JSON.parse(res.data.dataJson)) } catch { /* legacy metadata */ } } try { attachments.value = (await fileApi.list(entryId)).data || [] } catch { /* optional file service */ } isDirty.value = false } })
watch(() => [form.title, form.tags, form.status, form.type, form.contentMd], markDirty, { deep: true })
// 正文里的历史图片批量预热成 blob（已缓存的不会重复请求），就绪后 tick 驱动预览重渲染
watch(() => form.contentMd, (content) => { warmupImagesFromMarkdown(content || '').then(() => { imgTick.value++ }) })
</script>

<style scoped lang="scss">
.entry-edit { animation: page-fade 0.2s ease; padding-bottom: 92px; }.crumb { margin-bottom: 20px; }.editor-shell { overflow: hidden; }.editor-header { display: flex; justify-content: space-between; align-items: center; padding: 27px 32px 20px; border-bottom: 1px solid $border-soft; }.eyebrow { color: $primary; font-size: 11px; font-weight: 800; letter-spacing: 0.14em; }.editor-header h1 { margin-top: 7px; font-size: 24px; letter-spacing: -0.03em; }.save-state { color: $muted; font-size: 12px; white-space: nowrap; }.save-state.dirty { color: $warning; }.editor-form { padding: 26px 32px 32px; }.title-item { margin-bottom: 18px; }.title-item :deep(.el-form-item__label) { display: none; }.title-item :deep(.el-input__wrapper) { padding: 0; background: transparent; box-shadow: none; }.title-item :deep(.el-input__inner) { height: 52px; color: $ink; font-size: 32px; font-weight: 800; letter-spacing: -0.04em; }.title-item :deep(.el-input__inner)::placeholder { color: $faint; }.meta-row, .algorithm-row { display: flex; flex-wrap: wrap; gap: 12px 18px; }.meta-item { flex: 0 0 auto; margin-bottom: 10px; }.meta-tags { flex: 1 1 280px; }.meta-select { width: 130px; }.meta-item :deep(.el-form-item__label) { padding-bottom: 5px; color: $muted; font-size: 12px; line-height: 1.2; }.algorithm-row { padding: 12px 14px; margin: 2px 0 18px; background: $bg-subtle; border-radius: $radius-md; }.algorithm-row .el-input, .algorithm-row .el-select { width: 190px; }.mode-tabs { display: flex; gap: 4px; margin: 10px 0 12px; border-bottom: 1px solid $border-soft; }.mode-tabs button { padding: 9px 14px; border: 0; border-bottom: 2px solid transparent; background: transparent; color: $muted; cursor: pointer; font-weight: 600; }.mode-tabs button.active { color: $ink; border-bottom-color: $primary; }.content-item { margin-bottom: 10px; }.content-item :deep(.el-form-item__label) { color: $muted; font-size: 12px; }.editor-stage { display: grid; grid-template-columns: minmax(0, 1fr); min-height: 560px; border: 1px solid $border-soft; border-radius: $radius-md; overflow: hidden; }.editor-stage.mode-split { grid-template-columns: minmax(0, 1fr) minmax(0, 1fr); }.editor-stage.mode-preview { display: block; }.editor { min-height: 560px; border: 0; border-radius: 0; }.preview-pane { min-height: 560px; padding: 24px 28px; border-left: 1px solid $border-soft; overflow: auto; }.mode-preview .preview-pane { border-left: 0; }.material-bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 16px; }.material-hint { color: $muted; font-size: 12px; }.material-action { color: $primary; font-size: 13px; font-weight: 700; cursor: pointer; }.material-action input { display: none; }.attachment-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 12px; }.attachment-chip { display: inline-flex; align-items: center; gap: 7px; max-width: 280px; padding: 8px 10px; background: $bg-subtle; border-radius: $radius-sm; color: $ink-soft; font-size: 12px; }.attachment-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.attachment-size { color: $muted; }.save-bar { position: fixed; right: 0; bottom: 0; left: 0; z-index: 100; padding: 12px 20px; background: rgba(255,255,255,0.94); backdrop-filter: blur(10px); border-top: 1px solid $border-soft; }.save-bar-inner { display: flex; align-items: center; justify-content: space-between; max-width: 1120px; margin: 0 auto; }.save-actions { display: flex; gap: 10px; }.btn-ink:disabled { opacity: 0.55; cursor: not-allowed; }
@media (max-width: 900px) { .editor-stage.mode-split { grid-template-columns: 1fr; } .editor-stage.mode-split .preview-pane { border-top: 1px solid $border-soft; border-left: 0; } } @media (max-width: 768px) { .editor-header, .editor-form { padding-right: 16px; padding-left: 16px; }.editor-header h1 { font-size: 20px; }.title-item :deep(.el-input__inner) { font-size: 25px; }.editor-stage, .editor { min-height: 55vh; }.preview-pane { min-height: 55vh; padding: 18px; }.algorithm-row .el-input, .algorithm-row .el-select { width: 100%; }.save-bar { padding: 10px 16px; }.save-bar-inner .save-state { display: none; }.save-actions { width: 100%; }.save-actions > * { flex: 1; } } @keyframes page-fade { from { opacity: 0; } to { opacity: 1; } }
</style>

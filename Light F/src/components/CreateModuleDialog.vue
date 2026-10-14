<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑模块' : '创建模块'"
    width="480px"
    :close-on-click-modal="false"
  >
    <el-form :model="form" label-width="76px" ref="formRef" :rules="rules">
      <el-form-item label="封面图">
        <el-upload
          class="cover-uploader"
          :show-file-list="false"
          accept="image/png,image/jpeg,image/gif,image/webp"
          :before-upload="handleBeforeUpload"
          :http-request="handleCustomUpload"
        >
          <div v-if="form.coverImage" class="cover-preview">
            <img :src="coverPreviewUrl" alt="封面预览" />
            <div class="cover-mask">
              <span>点击更换图片</span>
            </div>
          </div>
          <div v-else v-loading="uploading" class="cover-placeholder">
            <span class="up-icon">
              <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
            </span>
            <span class="up-text">上传封面图</span>
            <span class="up-hint">jpg / png / gif / webp，≤ 5MB</span>
          </div>
        </el-upload>
        <button v-if="form.coverImage" class="cover-remove" @click="form.coverImage = ''">
          移除图片
        </button>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="form.name" placeholder="如：算法刷题" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="图标">
        <el-input v-model="form.icon" placeholder="emoji，如 🧮" maxlength="10" style="width: 140px" />
        <span class="tip">无封面图时显示</span>
      </el-form-item>
      <el-form-item label="主题色">
        <div class="color-list">
          <div
            v-for="c in colorOptions"
            :key="c"
            class="color-dot"
            :class="{ active: form.color === c }"
            :style="{ background: c }"
            @click="form.color = c"
          ></div>
        </div>
      </el-form-item>
      <el-form-item label="简介">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="3"
          placeholder="简单描述这个模块"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose" round>取消</el-button>
      <button class="btn-ink" @click="handleSubmit">
        {{ isEdit ? '保存修改' : '创建模块' }}
      </button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import type { Module } from '@/types'
import { uploadApi } from '@/api'
import { buildFileUrl, primeLocalFile, resolveDisplayUrl } from '@/utils/fileUrl'

const props = defineProps<{
  modelValue: boolean
  module?: Module | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'submit', data: Partial<Module>): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const isEdit = computed(() => !!props.module)

const colorOptions = [
  '#ea4c89', '#4f8cff', '#00b150', '#f7b500', '#7c5cff', '#0bb8b8', '#ff4d4f'
]

const formRef = ref<FormInstance>()
const uploading = ref(false)

const form = reactive<Partial<Module>>({
  name: '',
  icon: '📁',
  color: '#ea4c89',
  coverImage: '',
  description: ''
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入模块名称', trigger: 'blur' }]
}

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      if (props.module) {
        Object.assign(form, props.module)
      } else {
        form.name = ''
        form.icon = '📁'
        form.color = '#ea4c89'
        form.coverImage = ''
        form.description = ''
      }
    }
  }
)

// 封面图展示地址：库里存的是受保护稳定路径，<img> 必须用带 token 拉取的 blob URL
const coverPreviewUrl = ref('')
watch(
  () => form.coverImage,
  async (path) => {
    coverPreviewUrl.value = await resolveDisplayUrl(path)
  },
  { immediate: true }
)

const handleBeforeUpload = (rawFile: File) => {
  if (!rawFile.type.startsWith('image/')) {
    ElMessage.warning('只能上传图片文件')
    return false
  }
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.warning('图片大小不能超过 5MB')
    return false
  }
  return true
}

const handleCustomUpload = async (options: UploadRequestOptions) => {
  uploading.value = true
  try {
    const res = await uploadApi.image(options.file)
    // 入库保存稳定路径；本地 File 立即预热 blob 缓存，预览无需回拉
    form.coverImage = buildFileUrl(res.data.id)
    primeLocalFile(res.data.id, options.file)
    ElMessage.success('图片上传成功')
  } catch {
    ElMessage.error('图片上传失败，请重试')
  } finally {
    uploading.value = false
  }
}

const handleClose = () => {
  visible.value = false
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  emit('submit', { ...form })
  visible.value = false
}
</script>

<style scoped lang="scss">
.cover-uploader {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }
}

.cover-preview,
.cover-placeholder {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: $radius-md;
  overflow: hidden;
}

.cover-preview {
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }

  .cover-mask {
    position: absolute;
    inset: 0;
    background: rgba(13, 12, 34, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    opacity: 0;
    transition: opacity 0.2s;
  }

  &:hover .cover-mask {
    opacity: 1;
  }
}

.cover-placeholder {
  border: 1.5px dashed $border;
  background: $bg-subtle;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  cursor: pointer;
  transition: border-color 0.2s;

  &:hover {
    border-color: $faint;
  }

  .up-icon {
    color: $muted;
    display: flex;
  }
  .up-text {
    font-size: 13.5px;
    font-weight: 600;
    color: $ink-soft;
  }
  .up-hint {
    font-size: 12px;
    color: $faint;
  }
}

.cover-remove {
  margin-top: 8px;
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  color: $muted;
  cursor: pointer;

  &:hover {
    color: $danger;
  }
}

.tip {
  margin-left: 12px;
  font-size: 12px;
  color: $muted;
}

.color-list {
  display: flex;
  gap: 12px;
}

.color-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.2s;

  &.active {
    border-color: #fff;
    box-shadow: 0 0 0 2px $ink;
    transform: scale(1.12);
  }

  &:hover {
    transform: scale(1.12);
  }
}
</style>

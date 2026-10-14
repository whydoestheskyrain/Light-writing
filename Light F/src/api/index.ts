import request from './request'
import type { Module, Entry, FileMetadata, ApiResult, PageResult, User } from '@/types'

export const moduleApi = {
  list: () => request.get<unknown, ApiResult<Module[]>>('/module/list'),
  detail: (id: number) => request.get<unknown, ApiResult<Module>>(`/module/${id}`),
  create: (data: Partial<Module>) => request.post<unknown, ApiResult<void>>('/module', data),
  update: (id: number, data: Partial<Module>) =>
    request.put<unknown, ApiResult<void>>(`/module/${id}`, data),
  delete: (id: number) => request.delete<unknown, ApiResult<void>>(`/module/${id}`)
}

export const uploadApi = {
  image: (file: File, onProgress?: (percent: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<unknown, ApiResult<FileMetadata>>('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      }
    })
  }
}

export const fileApi = {
  upload: (file: File, entryId?: number, onProgress?: (percent: number) => void) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<unknown, ApiResult<FileMetadata>>('/file/upload', formData, {
      params: entryId ? { entryId } : undefined,
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (e) => {
        if (onProgress && e.total) onProgress(Math.round((e.loaded / e.total) * 100))
      }
    })
  },
  list: (entryId: number) => request.get<unknown, ApiResult<FileMetadata[]>>('/file/list', { params: { entryId } }),
  remove: (id: number) => request.delete<unknown, ApiResult<void>>(`/file/${id}`),
  // 受保护文件的“稳定路径”：用于入库 / 写入 Markdown 正文。
  // 切勿直接绑到 <img src>/<a href>（原生请求不带 JWT 会 401），
  // 展示/下载请用 @/utils/fileUrl 中的 resolveFileUrl / downloadAttachment。
  url: (id: number) => `/api/file/${id}/download`
}

export const entryApi = {
  list: (params: { current?: number; size?: number; moduleId?: number; status?: number; keyword?: string; tag?: string }) =>
    request.get<unknown, ApiResult<PageResult<Entry>>>('/entry/list', { params }),
  detail: (id: number) => request.get<unknown, ApiResult<Entry>>(`/entry/${id}`),
  create: (data: Partial<Entry>) => request.post<unknown, ApiResult<void>>('/entry', data),
  update: (id: number, data: Partial<Entry>) =>
    request.put<unknown, ApiResult<void>>(`/entry/${id}`, data),
  delete: (id: number) => request.delete<unknown, ApiResult<void>>(`/entry/${id}`),
  updateStatus: (id: number, status: number) =>
    request.patch<unknown, ApiResult<void>>(`/entry/${id}/status`, { status })
}

export const identityApi = {
  register: (data: { username: string; password: string; nickname?: string }) => request.post<unknown, ApiResult<any>>('/identity/register', data),
  login: (data: { username: string; password: string }) => request.post<unknown, ApiResult<any>>('/identity/login', data),
  refresh: (refreshToken: string) => request.post<unknown, ApiResult<any>>('/identity/refresh', { refreshToken }),
  me: () => request.get<unknown, ApiResult<User>>('/identity/me'),
  logout: () => request.post<unknown, ApiResult<void>>('/identity/logout')
}

export const learningApi = {
  heartbeat: (data: { eventId: string; seconds: number; moduleId?: number }) => request.post<unknown, ApiResult<void>>('/learning/heartbeat', data),
  summary: (range = 'today') => request.get<unknown, ApiResult<any>>('/learning/summary', { params: { range } }),
  records: () => request.get<unknown, ApiResult<any[]>>('/learning/records')
}

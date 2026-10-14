import type { AxiosResponse } from 'axios'
import request from '@/api/request'
import type { FileMetadata } from '@/types'

/**
 * 受保护文件的访问问题：
 * 下载接口 /api/file/{id}/download 走网关 JWT 校验，而 <img src> / <a href>
 * 这类浏览器原生请求不会携带 Authorization 头，直接访问必然 401。
 *
 * 解法：数据库 / Markdown 正文里始终保存“稳定路径”（对书签、分享、重渲染都稳定），
 * 展示层用带 token 的 axios 把文件拉成 Blob，再生成 blob: URL 赋给 <img>/<a>。
 * blob URL 按 fileId 全局缓存，同一文件只拉一次。
 */

// 稳定路径形如 /api/file/123/download；id 为字符串雪花 ID（19 位，超出 JS 安全整数）
const FILE_PATH_RE = /^\/?api\/file\/(\d+)\/download$/
// axios baseURL 已含 /api，发请求时要剥掉前缀，否则会拼成 /api/api/file/...
const stripApiPrefix = (path: string) => path.replace(/^\/api(?=\/)/, '')

// fileId -> blob:URL，会话级缓存（SPA 生命周期内复用，避免重复下载与闪烁）
const blobUrlCache = new Map<string, string>()
// 并发去重：同一 fileId 的多个组件同时解析时只发一次请求
const inflight = new Map<string, Promise<string>>()

/** 稳定路径：入库、写入 Markdown 正文都用它 */
export const buildFileUrl = (id: number | string): string => `/api/file/${id}/download`

export const fileIdFromUrl = (url: string): string | null => FILE_PATH_RE.exec(url)?.[1] ?? null

/**
 * 上传场景预热：本地刚选中的 File 对象可以直接生成 blob URL，
 * 让编辑器/封面预览在上传成功的瞬间就能显示，无需再向服务器回拉一次。
 */
export function primeLocalFile(id: number | string, file: Blob): void {
  const key = String(id)
  if (!blobUrlCache.has(key)) blobUrlCache.set(key, URL.createObjectURL(file))
}

/** 带鉴权拉取文件，返回可直接绑定到 <img src> 的 blob: URL，按 fileId 缓存 */
export function resolveFileUrl(id: number | string): Promise<string> {
  const key = String(id)
  const cached = blobUrlCache.get(key)
  if (cached) return Promise.resolve(cached)

  const pending = inflight.get(key)
  if (pending) return pending

  const task = (async () => {
    // request 的响应拦截器对 responseType=blob 直接放行完整 AxiosResponse
    // 剥掉 /api 前缀：axios baseURL 已是 /api
    const res = (await request.get(stripApiPrefix(buildFileUrl(key)), { responseType: 'blob' })) as unknown as AxiosResponse<Blob>
    const url = URL.createObjectURL(res.data)
    blobUrlCache.set(key, url)
    inflight.delete(key)
    return url
  })()

  inflight.set(key, task)
  return task
}

/** 组件 <img :src> 绑定用：http/data/blob 地址原样返回，受保护路径解析为 blob URL */
export async function resolveDisplayUrl(path?: string | null): Promise<string> {
  if (!path) return ''
  if (/^(https?:|data:|blob:)/.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  const id = fileIdFromUrl(normalized)
  if (!id) return normalized
  try {
    return await resolveFileUrl(id)
  } catch {
    // 401/404/网络错误等：返回空串，<img> 不渲染，组件降级显示占位
    return ''
  }
}

/**
 * md-editor-v3 的 sanitize 是同步钩子 (html) => html，
 * 只能替换“已在缓存中”的 URL（刚上传的图片经 primeLocalFile 预热、
 * 历史图片经 warmupImagesFromMarkdown 预热后即命中）。未命中保持原路径。
 */
export function replaceCachedFileUrls(html: string): string {
  return html.replace(/\/?api\/file\/(\d+)\/download/g, (match, id: string) => blobUrlCache.get(id) ?? match)
}

/** v-html 渲染后调用：把容器内所有受保护 <img> 的 src 异步换成 blob URL */
export async function hydrateProtectedImages(container: HTMLElement | null): Promise<void> {
  if (!container) return
  const imgs = Array.from(container.querySelectorAll<HTMLImageElement>('img[src]'))
  await Promise.all(
    imgs.map(async (img) => {
      const id = fileIdFromUrl(img.getAttribute('src') || '')
      if (!id || img.dataset.hydrated === id) return
      try {
        img.src = await resolveFileUrl(id)
        img.dataset.hydrated = id
      } catch {
        // 拉取失败（权限/网络）时保留原 src，交给浏览器的裂图占位
      }
    })
  )
}

/** 从 Markdown 正文中提取所有受保护图片的 fileId，用于进入编辑页时批量预热缓存 */
export function extractFileIds(markdown: string): string[] {
  return Array.from(markdown.matchAll(/\/?api\/file\/(\d+)\/download/g)).map((m) => m[1])
}

/** 批量预热正文中的历史图片；全部就绪后 resolve，组件据此触发一次重新渲染 */
export async function warmupImagesFromMarkdown(markdown: string): Promise<void> {
  await Promise.all(extractFileIds(markdown).map((id) => resolveFileUrl(id).catch(() => undefined)))
}

/**
 * 附件下载：替代 <a href> 直链（直链不带 token 会 401）。
 * 拉成 blob 后用临时 <a download> 触发浏览器保存/打开。
 */
export async function downloadAttachment(file: FileMetadata): Promise<void> {
  const url = await resolveFileUrl(file.id)
  const a = document.createElement('a')
  a.href = url
  a.download = file.originalName
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  a.remove()
}

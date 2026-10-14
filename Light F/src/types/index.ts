// 后端主键为雪花算法生成的 Long（19 位），超出 JS 安全整数范围，
// 统一由 Jackson 序列化为字符串，前端一律按 string 处理，禁止再用 Number() 转换。
export type ID = string

export interface Module {
  id: ID
  name: string
  icon: string
  color: string
  coverImage: string
  description: string
  sortOrder: number
  createTime: string
  updateTime: string
}

export interface Entry {
  id: ID
  moduleId: ID
  type: EntryType
  title: string
  contentMd: string
  dataJson: string
  status: EntryStatus
  tags: string
  nextReviewTime: string
  createTime: string
  updateTime: string
}

export interface FileMetadata {
  id: ID
  entryId?: ID | null
  originalName: string
  objectKey: string
  contentType: string
  fileSize: number
  sha256: string
  createTime: string
}

export type EntryType = 'note' | 'algorithm' | 'qa' | 'link'

export type EntryStatus = 0 | 1 | 2 // 0未做 1在做 2已掌握

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}
export interface PageResult<T> { records: T[]; total: number; current: number; size: number; pages: number }
export interface User { id: ID; username: string; nickname: string; role: string }

export interface AlgorithmData {
  leetcodeId?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  complexity?: string
}

export interface Module {
  id: number
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
  id: number
  moduleId: number
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
  id: number
  entryId?: number | null
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
export interface User { id: number; username: string; nickname: string; role: string }

export interface AlgorithmData {
  leetcodeId?: string
  difficulty?: 'easy' | 'medium' | 'hard'
  complexity?: string
}

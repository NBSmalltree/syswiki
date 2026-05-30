export type ModuleType =
  | 'INTRO'
  | 'ARCH_TEST'
  | 'ARCH_PROD'
  | 'SERVER'
  | 'NETWORK'
  | 'DATABASE'
  | 'GUIDE'

export interface ContentItem {
  contentId: string
  systemId: string
  moduleType: ModuleType
  mdContent: string
  version: number
  operator: string
  createTime: string
  updateTime: string
}

export interface ContentSaveDTO {
  mdContent: string
  operator: string
}

export interface ContentVersion {
  versionId: string
  version: number
  operator: string
  createTime: string
}

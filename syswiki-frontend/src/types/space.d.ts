export interface Space {
  systemId: string
  systemName: string
  systemCode: string
  owner: string
  description: string
  status: string
  createTime: string
  updateTime: string
}

export interface CreateSpaceForm {
  systemName: string
  systemCode: string
  owner: string
  description: string
}

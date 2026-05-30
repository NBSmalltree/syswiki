export interface SqlParam { name: string; label: string; placeholder?: string }
export interface SqlItem {
  sqlId: string; systemId: string; title: string; category: string;
  sqlTemplate: string; description: string; params: SqlParam[];
  sortOrder: number; operator: string; createTime: string; updateTime: string;
}

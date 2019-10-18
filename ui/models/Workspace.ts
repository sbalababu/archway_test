export interface YarnApplication {
  id: string;
  name: string;
  start_time?: Date;
}

export interface ResourcePoolsInfo {
  loading: boolean;
  error?: string;
  data: PoolInfo[];
}

export interface PoolInfo {
  resource_pool: string;
  applications: YarnApplication[];
}

export interface NamespaceInfo {
  name: string;
  tables: HiveTable[];
}

export interface NamespaceInfoList {
  loading: boolean;
  error?: string;
  data: NamespaceInfo[];
}

export interface HiveTable {
  name: string;
}

export interface MemberRemoveStatus {
  loading?: boolean;
  success?: boolean;
  error?: boolean;
}

export interface Member {
  distinguished_name: string;
  email?: string;
  name: string;
  removeStatus?: MemberRemoveStatus;
  data: any;
  topics: any;
  applications: any;
}

export interface Application {
  id: number;
  name: string;
  consumer_group: string;
  group: SecurityGroup;
  type?: string;
  logo?: string;
  language?: string;
  repository?: string;
}

export interface TopicGrant {
  id: number;
  group: SecurityGroup;
  actions: string;
  topic_access?: Date;
}

export interface KafkaTopic {
  id: number;
  name: string;
  partitions: number;
  replication_factor: number;
  managing_role: TopicGrant;
  readonly_role: TopicGrant;
}

export interface ResourcePool {
  id: number;
  pool_name: string;
  max_cores: number;
  max_memory_in_gb: number;
}

export interface SecurityGroup {
  common_name: string;
  distinguished_name: string;
  sentry_role: string;
  group_created?: Date;
  role_created?: Date;
  role_associated?: Date;
  attributes?: string[][];
}

export interface Compliance {
  phi_data?: boolean;
  pci_data?: boolean;
  pii_data?: boolean;
}

export interface DatabaseGrant {
  group: SecurityGroup;
  location_access?: Date;
  database_access?: Date;
}

export interface HiveAllocation {
  id: number;
  name: string;
  location: string;
  size_in_gb: number;
  consumed_in_gb: number;
  managing_group: DatabaseGrant;
  readwrite_group?: DatabaseGrant;
  readonly_group?: DatabaseGrant;
}

export interface ApprovalStatus {
  loading?: boolean;
  success?: boolean;
  error?: string;
}

export interface ApprovalItem {
  approver?: string;
  approval_time?: Date;
  status?: ApprovalStatus;
}

export interface Approvals {
  infra?: ApprovalItem;
  risk?: ApprovalItem;
}

export interface Workspace {
  id: number;
  name: string;
  summary: string;
  description: string;
  behavior: string;
  requested_date: Date;
  requester: string;
  single_user: boolean;
  status?: string;
  compliance: Compliance;
  approvals?: Approvals;
  data: HiveAllocation[];
  processing: ResourcePool[];
  topics: KafkaTopic[];
  applications: Application[];
}

export interface UserSuggestion {
  display: string;
  distinguished_name: string;
}

export interface UserSuggestions {
  filter: string;
  users: UserSuggestion[];
  groups: UserSuggestion[];
}

export interface WorkspaceSearchResult {
  id: number;
  name: string;
  summary: string;
  status: string;
  behavior: string;
  pii_data?: boolean;
  pci_data?: boolean;
  phi_data?: boolean;
  date_requested: Date;
  date_fully_approved: Date;
  total_disk_allocated_in_gb: number;
  total_disk_consumed_in_gb?: number;
  total_max_cores: number;
  total_max_memory_in_gb: number;
}

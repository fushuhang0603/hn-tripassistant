// 与后端统一响应体 Result<T> 对齐
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

// 登录成功后返回的数据（对应后端 LoginVO）
export interface LoginResult {
  token: string
  userInfoVO: UserInfo
}

// 用户信息（对应后端 UserInfoVO，id 由后端序列化为字符串）
export interface UserInfo {
  id: string
  username: string
  status: number
  role: string
}

// 登录请求参数
export interface LoginParams {
  username: string
  password: string
}

// 注册请求参数
export interface RegisterParams {
  username: string
  password: string
  confirmPassword: string
}

// ===== 统一聊天接口（/api/chat/send） =====

// 聊天请求（首次 sessionId 为空，后端创建后随响应返回，后续轮次原样带回）
export interface ChatRequest {
  sessionId?: string
  message: string
}

// 聊天响应（非流式版本，兼容保留）
export interface ChatResponse {
  sessionId: string
  intent: string
  reply: string
  askMessage: string
  complete: boolean
  data: unknown
}

// SSE 流式事件（/api/chat/send/stream）
// meta：会话元信息 / token：文本增量 / ask：追问 / error：错误兜底 / end：结束+结构化数据
export interface StreamEvent {
  type: 'meta' | 'token' | 'ask' | 'error' | 'end'
  sessionId?: string
  intent?: string
  content?: string
  reply?: string
  askMessage?: string
  data?: unknown
}

// ===== AI 行程规划（对齐后端 graph.plan 模型） =====

// 活动
export interface ActivityPlan {
  name: string
  description: string
}

// 餐饮推荐
export interface MealPlan {
  type: string
  name: string
  location: string
  address: string
  city: string
}

// 景点
export interface SpotPlan {
  name: string
  location: string
  address: string
  poiId: string
  city: string
  duration: string
  intro: string
}

// 单日行程
export interface DayPlan {
  dayIndex: number
  theme: string
  spots: SpotPlan[]
  activities: ActivityPlan[]
  meals: MealPlan[]
  transport: string
  stayTip: string
}

// 行程规划结果
export interface TripPlan {
  title: string
  summary: string
  budgetTip: string
  days: DayPlan[]
}

// /graph/planTrip 参数不完整时的追问响应
export interface PlanTripAsk {
  complete: false
  askMessage: string
}

// /graph/planTrip 返回：追问 或 完整行程
export type PlanTripResult = PlanTripAsk | TripPlan

// ===== 攻略广场（对齐后端 business.domain） =====

// 攻略卡片（对应 GuideCardVO，id 由后端序列化为字符串）
export interface GuideCard {
  id: string
  title: string
  cover: string
  city: string
  days: string
  budget: string
  tags: string[]
  authorName: string
  likes: number
  source: string
  createTime: string
}

// 攻略详情（对应 GuideDetailVO）
export interface GuideDetail extends GuideCard {
  content: string
  images: string[]
  liked: boolean
  tripPlan: unknown
}

// 攻略分页查询参数（对应 GuidePageQueryDTO）
export interface GuidePageQuery {
  city?: string
  tag?: string
  keyword?: string
  sort?: 'hot' | 'new'
  page?: number
  size?: number
}

// 发布攻略参数（对应 GuidePublishDTO）
export interface GuidePublishParams {
  title: string
  cover?: string
  city: string
  days: string
  budget: string
  tags: string[]
  content: string
  images: string[]
  source?: string
  tripPlan?: unknown
}

// 通用分页返回（对应 PageResult）
export interface PageResult<T> {
  records: T[]
  total: number
}



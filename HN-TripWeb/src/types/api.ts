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

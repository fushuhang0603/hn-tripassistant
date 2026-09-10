import type { StreamEvent } from '../types/api'

const TOKEN_KEY = 'token'

// 统一请求封装：业务代码通过泛型拿到具体类型，避免隐式 any
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem(TOKEN_KEY)
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // 后端 LoginInterceptor 校验 Bearer token（登录接口本身不带也无妨）
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
    if (!window.location.pathname.startsWith('/login')) {
      window.location.replace('/login')
    }
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }

  // API 响应是运行时数据，这里是信任后端契约的边界转换
  return (await response.json()) as T
}

export function post<T>(url: string, body: unknown): Promise<T> {
  return request<T>(url, {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export function get<T>(url: string): Promise<T> {
  return request<T>(url)
}

export async function getText(url: string): Promise<string> {
  const response = await fetch(url)
  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.text()
}

// SSE 流式请求：POST 后逐帧解析，每个事件回调一次（事件为 StreamEvent JSON）
export async function postStream(
  url: string,
  body: unknown,
  onEvent: (event: StreamEvent) => void,
): Promise<void> {
  const token = localStorage.getItem(TOKEN_KEY)
  const response = await fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(body),
  })

  if (response.status === 401) {
    localStorage.removeItem(TOKEN_KEY)
    if (!window.location.pathname.startsWith('/login')) {
      window.location.replace('/login')
    }
    throw new Error('登录已过期，请重新登录')
  }

  if (!response.ok || !response.body) {
    throw new Error(`请求失败：${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    // SSE 帧以空行分隔，取 data: 行解析
    let sep = buffer.indexOf('\n\n')
    while (sep !== -1) {
      const frame = buffer.slice(0, sep)
      buffer = buffer.slice(sep + 2)

      const dataLine = frame.split('\n').find((line) => line.startsWith('data:'))
      if (dataLine) {
        const payload = dataLine.slice(5).trim()
        if (payload && payload !== '[DONE]') {
          try {
            onEvent(JSON.parse(payload) as StreamEvent)
          } catch {
            // 忽略无法解析的帧
          }
        }
      }
      sep = buffer.indexOf('\n\n')
    }
  }
}

// 统一请求封装：业务代码通过泛型拿到具体类型，避免隐式 any
async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })

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

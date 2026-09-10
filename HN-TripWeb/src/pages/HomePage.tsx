import { useEffect, useRef, useState } from 'react'
import { App } from 'antd'
import {
  CarOutlined,
  ClockCircleOutlined,
  CoffeeOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  HomeOutlined,
  StarOutlined,
} from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { postStream } from '../lib/request'
import type { StreamEvent, TripPlan } from '../types/api'
import './HomePage.css'

interface ChatMessage {
  id: number
  role: 'user' | 'assistant'
  content: string
  tripPlan?: TripPlan
}

interface SpotItem {
  name: string
  desc: string
  text: string
}

const spots: SpotItem[] = [
  { name: '三亚 · 亚龙湾', desc: '细腻白沙滩，潜水度假，看海上日落', text: '帮我规划三亚3天旅游路线' },
  { name: '万宁 · 石梅湾', desc: '冲浪圣地，椰林公路，咖啡庄园打卡', text: '万宁有什么好玩的' },
  { name: '海口 · 骑楼老街', desc: '南洋复古街区，地道小吃聚集地', text: '海口一日游攻略' },
  { name: '陵水 · 分界洲岛', desc: '玻璃海，潜水、海上娱乐项目', text: '陵水旅游推荐' },
]

const quickCommands = [
  '海南5天4晚完整行程',
  '海南必吃美食清单',
  '出行天气与穿搭建议',
  '自驾路线注意事项',
  '预算三千怎么玩海南',
]

function extractTripPlan(data: unknown): TripPlan | null {
  if (data && typeof data === 'object' && 'days' in data && Array.isArray((data as TripPlan).days)) {
    return data as TripPlan
  }
  return null
}

function TripPlanCard({ plan }: { plan: TripPlan }) {
  const days = plan.days ?? []
  return (
    <div className="trip-card">
      <div className="trip-card__header">
        {plan.title && <h2 className="trip-card__title">{plan.title}</h2>}
        {plan.summary && <p className="trip-card__summary">{plan.summary}</p>}
        {plan.budgetTip && (
          <div className="trip-card__budget">
            <span className="trip-icon trip-icon--gold">
              <DollarOutlined />
            </span>
            <span>{plan.budgetTip}</span>
          </div>
        )}
      </div>

      {days.map((day) => (
        <section key={day.dayIndex} className="trip-day">
          <div className="trip-day__head">
            <span className="trip-day__index">第 {day.dayIndex} 天</span>
            {day.theme && <span className="trip-day__theme">{day.theme}</span>}
          </div>

          {(day.spots ?? []).length > 0 && (
            <div className="trip-section">
              <div className="trip-section__title">
                <span className="trip-icon trip-icon--blue">
                  <EnvironmentOutlined />
                </span>
                景点
              </div>
              <div className="trip-spots">
                {day.spots.map((s, i) => (
                  <div key={i} className="trip-spot">
                    <div className="trip-spot__name">
                      <span className="trip-spot__num">{i + 1}</span>
                      {s.name}
                    </div>
                    {s.duration && (
                      <div className="trip-spot__meta">
                        <ClockCircleOutlined /> {s.duration}
                      </div>
                    )}
                    {s.address && (
                      <div className="trip-spot__meta trip-spot__meta--addr">
                        <EnvironmentOutlined /> {s.address}
                      </div>
                    )}
                    {s.intro && <div className="trip-spot__intro">{s.intro}</div>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {(day.activities ?? []).length > 0 && (
            <div className="trip-section">
              <div className="trip-section__title">
                <span className="trip-icon trip-icon--cyan">
                  <StarOutlined />
                </span>
                活动
              </div>
              <ul className="trip-list">
                {day.activities.map((a, i) => (
                  <li key={i}>
                    <span className="trip-list__name">{a.name}</span>
                    {a.description && <span className="trip-list__desc">{a.description}</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {(day.meals ?? []).length > 0 && (
            <div className="trip-section">
              <div className="trip-section__title">
                <span className="trip-icon trip-icon--orange">
                  <CoffeeOutlined />
                </span>
                餐饮
              </div>
              <ul className="trip-list">
                {day.meals.map((m, i) => (
                  <li key={i}>
                    <span className="trip-list__name">
                      {m.type ? `${m.type} · ` : ''}
                      {m.name}
                    </span>
                    {m.address && <span className="trip-list__desc trip-list__desc--addr">{m.address}</span>}
                  </li>
                ))}
              </ul>
            </div>
          )}

          {day.transport && (
            <div className="trip-section">
              <div className="trip-section__title">
                <span className="trip-icon trip-icon--green">
                  <CarOutlined />
                </span>
                交通
              </div>
              <div className="trip-note">{day.transport}</div>
            </div>
          )}

          {day.stayTip && (
            <div className="trip-section">
              <div className="trip-section__title">
                <span className="trip-icon trip-icon--purple">
                  <HomeOutlined />
                </span>
                住宿
              </div>
              <div className="trip-note">{day.stayTip}</div>
            </div>
          )}
        </section>
      ))}
    </div>
  )
}

function HomePage() {
  const { message } = App.useApp()
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      role: 'assistant',
      content:
        '👋 系统已接入海南旅游知识库。\n\n我可以根据目的地、出行天数、预算和偏好，生成路线规划、景点推荐、美食清单、天气建议和自驾方案。\n\n请输入你的旅行需求，例如：三亚4天怎么玩？',
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const listRef = useRef<HTMLDivElement>(null)
  const sessionIdRef = useRef('')

  useEffect(() => {
    const el = listRef.current
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  }, [messages, loading])

  const sendMessage = async (question: string) => {
    const text = question.trim()
    if (!text || loading) return

    setInput('')
    // 先压入用户消息 + 一条空白的 assistant 占位消息，token 事件增量填充
    setMessages((prev) => [
      ...prev,
      { id: Date.now(), role: 'user', content: text },
      { id: Date.now() + 1, role: 'assistant', content: '' },
    ])
    setLoading(true)

    // 改写最后一条 assistant 消息的 content
    const patchAssistant = (patch: (content: string) => string) => {
      setMessages((prev) => {
        const last = prev[prev.length - 1]
        if (!last || last.role !== 'assistant') return prev
        return [...prev.slice(0, -1), { ...last, content: patch(last.content) }]
      })
    }

    try {
      await postStream(
        '/api/chat/send/stream',
        { sessionId: sessionIdRef.current || '', message: text },
        (event: StreamEvent) => {
          switch (event.type) {
            case 'meta':
              if (event.sessionId) {
                sessionIdRef.current = event.sessionId
              }
              break
            case 'token':
              patchAssistant((content) => content + (event.content ?? ''))
              break
            case 'ask':
              patchAssistant(() => event.askMessage || event.reply || '请补充信息后再试')
              break
            case 'error':
              patchAssistant(() => event.content || '出错了，请稍后重试')
              break
            case 'end': {
              // 结构化数据：行程规划类则挂 tripPlan 卡片
              const tripPlan = extractTripPlan(event.data)
              if (tripPlan) {
                setMessages((prev) => {
                  const last = prev[prev.length - 1]
                  if (!last || last.role !== 'assistant') return prev
                  return [...prev.slice(0, -1), { ...last, tripPlan }]
                })
              }
              break
            }
          }
        },
      )
    } catch {
      message.error('网络错误，请稍后重试')
      patchAssistant((content) => content || '网络异常，请稍后重试')
    } finally {
      setLoading(false)
    }
  }

  const handleSend = () => {
    void sendMessage(input)
  }

  return (
    <div className="home-container">
      <aside className="sidebar">
        <div className="section-title">Destinations</div>
        <h3 className="sidebar-heading">🏝️ 热门目的地</h3>

        {spots.map((spot) => (
          <div key={spot.name} className="spot-item" onClick={() => sendMessage(spot.text)}>
            <div className="spot-name">{spot.name}</div>
            <div className="spot-desc">{spot.desc}</div>
          </div>
        ))}

        <div className="quick-question">
          <div className="section-title">Quick Commands</div>
          <h3 className="sidebar-heading">💡 快捷指令</h3>

          {quickCommands.map((cmd) => (
            <button key={cmd} type="button" className="quick-btn" onClick={() => sendMessage(cmd)}>
              {cmd}
            </button>
          ))}
        </div>
      </aside>

      <div className="chat-wrap">
        <div className="chat-box" ref={listRef}>
          {messages.map((msg) => {
            if (msg.role === 'user') {
              return (
                <div key={msg.id} className="msg-item msg-user">
                  <div className="msg-bubble">{msg.content}</div>
                </div>
              )
            }
            if (msg.tripPlan) {
              return (
                <div key={msg.id} className="msg-item msg-ai msg-item--plan">
                  <TripPlanCard plan={msg.tripPlan} />
                </div>
              )
            }
            return (
              <div key={msg.id} className="msg-item msg-ai">
                <div className="msg-bubble">
                  <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content}</ReactMarkdown>
                </div>
              </div>
            )
          })}
          {loading && !messages[messages.length - 1]?.content && (
            <div className="msg-item msg-ai">
              <div className="msg-bubble msg-bubble-loading">正在思考中…</div>
            </div>
          )}
        </div>

        <div className="input-area">
          <textarea
            id="inputText"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="输入指令，例如：帮我生成一份5天4晚海南环岛方案"
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault()
                handleSend()
              }
            }}
            disabled={loading}
          />
          <button id="sendBtn" type="button" onClick={handleSend} disabled={loading}>
            发送
          </button>
        </div>
      </div>
    </div>
  )
}

export default HomePage

import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { App, Button, Empty, Spin } from 'antd'
import {
  ArrowLeftOutlined,
  ClockCircleOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  HeartOutlined,
  PictureOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { get } from '../lib/request'
import type { ApiResponse, GuideDetail } from '../types/api'
import './GuideDetail.css'

const sourceLabel = (source?: string) => (source === 'AI_TRIP' ? 'AI行程一键发布' : '用户发布')

const formatTime = (s?: string) => (s ? s.replace('T', ' ') : '')

const formatCount = (n: number) => (n >= 1000 ? `${(n / 1000).toFixed(1)}k` : `${n}`)

// 后端 tripPlan 为 AI 行程快照（用户发布时为 null），可能是对象或 JSON 字符串
function renderTripPlan(tripPlan: unknown): string {
  if (tripPlan == null) return ''
  if (typeof tripPlan === 'string') {
    try {
      return JSON.stringify(JSON.parse(tripPlan), null, 2)
    } catch {
      return tripPlan
    }
  }
  return JSON.stringify(tripPlan, null, 2)
}

function GuideDetailPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const { id } = useParams<{ id: string }>()
  const [detail, setDetail] = useState<GuideDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    const load = async () => {
      setLoading(true)
      try {
        const res = await get<ApiResponse<GuideDetail>>(`/api/guide/detail/${id}`)
        if (cancelled) return
        if (res.code === 200) {
          setDetail(res.data)
        } else {
          message.error(res.message || '加载失败')
        }
      } catch {
        if (!cancelled) message.error('加载攻略详情失败')
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => {
      cancelled = true
    }
  }, [id, message])

  if (loading) {
    return (
      <div className="gd-page">
        <div className="gd-loading">
          <Spin size="large" />
        </div>
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="gd-page">
        <Empty description="攻略不存在或已删除" style={{ marginTop: 120 }} />
      </div>
    )
  }

  const tripPlanText = renderTripPlan(detail.tripPlan)

  return (
    <div className="gd-page">
      <header className="gd-topbar">
        <Button
          type="text"
          icon={<ArrowLeftOutlined />}
          onClick={() => navigate('/square')}
        >
          返回攻略广场
        </Button>
        <Button type="primary" onClick={() => navigate('/square/publish')}>
          发布我的攻略
        </Button>
      </header>

      <div className="gd-container">
        {detail.cover && (
          <div className="gd-cover">
            <img src={detail.cover} alt={detail.title} />
          </div>
        )}

        <h1 className="gd-title">{detail.title}</h1>

        <div className="gd-meta">
          <span className="gd-meta-item">
            <EnvironmentOutlined /> {detail.city}
          </span>
          <span className="gd-meta-item">
            <ClockCircleOutlined /> {detail.days}
          </span>
          <span className="gd-meta-item">
            <DollarOutlined /> {detail.budget}
          </span>
          <span className="gd-meta-item">
            <UserOutlined /> {detail.authorName}
          </span>
        </div>

        {detail.tags?.length > 0 && (
          <div className="gd-tags">
            {detail.tags.map((t) => (
              <span key={t} className="gd-tag">
                {t}
              </span>
            ))}
          </div>
        )}

        <div className="gd-submeta">
          <span>发布于 {formatTime(detail.createTime)}</span>
          <span className="gd-submeta-dot">·</span>
          <span>{sourceLabel(detail.source)}</span>
          <span className="gd-submeta-dot">·</span>
          <span className="gd-like-count">
            <HeartOutlined /> {formatCount(detail.likes)} 赞
          </span>
        </div>

        <section className="gd-section">
          <h2 className="gd-section-title">攻略正文</h2>
          <div className="gd-content">{detail.content || '暂无正文'}</div>
        </section>

        {detail.images?.length > 0 && (
          <section className="gd-section">
            <h2 className="gd-section-title">
              <PictureOutlined /> 图集（{detail.images.length}）
            </h2>
            <div className="gd-gallery">
              {detail.images.map((img, idx) => (
                <img key={`${img}-${idx}`} src={img} alt={`图${idx + 1}`} loading="lazy" />
              ))}
            </div>
          </section>
        )}

        {tripPlanText && (
          <section className="gd-section">
            <h2 className="gd-section-title">AI 行程快照</h2>
            <pre className="gd-trip-plan">{tripPlanText}</pre>
          </section>
        )}
      </div>
    </div>
  )
}

export default GuideDetailPage

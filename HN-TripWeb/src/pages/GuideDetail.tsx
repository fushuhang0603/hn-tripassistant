import { useEffect, useState } from 'react'
import { App, Button, Carousel, Empty, Spin } from 'antd'
import {
  ClockCircleOutlined,
  CloseOutlined,
  DollarOutlined,
  EnvironmentOutlined,
  HeartOutlined,
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

interface GuideDetailModalProps {
  id: string
  onClose: () => void
}

function GuideDetailModal({ id, onClose }: GuideDetailModalProps) {
  const { message } = App.useApp()
  const [detail, setDetail] = useState<GuideDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    const load = async () => {
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
      <div className="gd-overlay" onClick={onClose}>
        <Spin size="large" />
      </div>
    )
  }

  if (!detail) {
    return (
      <div className="gd-overlay" onClick={onClose}>
        <div className="gd-empty">
          <Empty description="攻略不存在或已删除" />
          <Button type="primary" onClick={onClose}>
            返回攻略广场
          </Button>
        </div>
      </div>
    )
  }

  const tripPlanText = renderTripPlan(detail.tripPlan)

  // 封面 + 图集合并为轮播图，按出现顺序去重
  const allImages = [detail.cover, ...(detail.images ?? [])].filter(
    (src, i, arr) => src && arr.indexOf(src) === i,
  ) as string[]

  return (
    <div className="gd-overlay" onClick={onClose}>
      <div className="gd-dialog" onClick={(e) => e.stopPropagation()}>
        <Button
          type="text"
          shape="circle"
          className="gd-close"
          icon={<CloseOutlined />}
          aria-label="关闭"
          onClick={onClose}
        />

        <div className="gd-dialog-body">
          {allImages.length > 0 && (
            <div className="gd-carousel">
              <Carousel
                arrows={allImages.length > 1}
                dots={allImages.length > 1}
                autoplay={false}
              >
                {allImages.map((src, idx) => (
                  <div key={src} className="gd-carousel-item">
                    <img src={src} alt={`${detail.title}-${idx + 1}`} />
                  </div>
                ))}
              </Carousel>
              {allImages.length > 1 && (
                <span className="gd-carousel-count">{allImages.length} 张图片</span>
              )}
            </div>
          )}

          <h1 className="gd-title">{detail.title}</h1>

          {detail.tags?.length > 0 && (
            <div className="gd-tags">
              {detail.tags.map((t) => (
                <span key={t} className="gd-tag">
                  {t}
                </span>
              ))}
            </div>
          )}

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

          {tripPlanText && (
            <section className="gd-section">
              <h2 className="gd-section-title">AI 行程快照</h2>
              <pre className="gd-trip-plan">{tripPlanText}</pre>
            </section>
          )}
        </div>
      </div>
    </div>
  )
}

export default GuideDetailModal

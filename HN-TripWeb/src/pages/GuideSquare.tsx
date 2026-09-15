import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Avatar, Carousel, Dropdown, Empty, Input, Spin } from 'antd'
import type { MenuProps } from 'antd'
import {
  CompassOutlined,
  DownOutlined,
  FireOutlined,
  HeartOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import { post } from '../lib/request'
import type { ApiResponse, GuideCard, GuidePageQuery, PageResult } from '../types/api'
import './GuideSquare.css'

const img = (prompt: string, size = 'landscape_16_9') =>
  `https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=${encodeURIComponent(
    prompt,
  )}&image_size=${size}`

const penguin = img(
  'cute cartoon penguin wearing sunglasses standing on tropical beach with coconut palm tree, 2D flat vector illustration, mint green and light blue color palette, clean white background, kawaii minimal',
  'square',
)

const fallbackCover = img('Hainan tropical island beach coconut palm turquoise sea aerial view')

const banners = [
  {
    key: '1',
    img: img('Hainan Sanya beach coconut palms turquoise sea aerial drone view bright sunlight'),
    title: '发现海南好行程',
    sub: '来自真实用户的海岛旅行灵感，也可以用AI一键生成你的专属路线',
  },
  {
    key: '2',
    img: img('Wanning Shimei Bay surfing beach waves coconut road blue sea sunny day'),
    title: '乘风破浪，海岛冲浪季',
    sub: '万宁石梅湾、日月湾，等你来挑战第一道浪',
  },
  {
    key: '3',
    img: img('Haikou Qilou old street Nanyang style arcade buildings nostalgic sunny day'),
    title: '椰城慢生活',
    sub: '海口骑楼老街，用一天读懂南洋风情',
  },
]

// 侧边栏前 4 项为城市，其余为标签，用于区分查询参数
const CITY_KEYS = new Set(['三亚', '万宁', '海口', '琼海'])

const sideMenu = [
  { key: 'recommend', label: '推荐攻略' },
  { key: '三亚', label: '三亚' },
  { key: '万宁', label: '万宁' },
  { key: '海口', label: '海口' },
  { key: '琼海', label: '琼海' },
  { key: '亲子游', label: '亲子游' },
  { key: '海边度假', label: '海边度假' },
  { key: '美食探店', label: '美食探店' },
]

const hotRoutes = [
  { icon: '🏄', color: '#40b8e8', title: '万宁冲浪3天2晚', desc: '石梅湾 · 日月湾' },
  { icon: '🍜', color: '#86ddbc', title: '海口美食周末游', desc: '骑楼老街 · 地道小吃' },
  { icon: '💑', color: '#ffb86b', title: '三亚蜜月5天', desc: '亚龙湾 · 海棠湾' },
]

const aiMenu: MenuProps['items'] = [
  { key: 'ai-new', label: '生成新行程' },
  { key: 'ai-history', label: '历史生成记录' },
]
const tripMenu: MenuProps['items'] = [
  { key: 'trip-ongoing', label: '进行中的行程' },
  { key: 'trip-done', label: '已完成的行程' },
]
const publishMenu: MenuProps['items'] = [
  { key: 'pub-photo', label: '发布图文攻略' },
  { key: 'pub-video', label: '发布视频攻略' },
]

const formatCount = (n: number) => (n >= 1000 ? `${(n / 1000).toFixed(1)}k` : `${n}`)

interface GuideSquareProps {
  onLogout: () => void
}

function GuideSquare({ onLogout }: GuideSquareProps) {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [activeMenu, setActiveMenu] = useState('recommend')
  const [sort, setSort] = useState<'new' | 'hot'>('new')
  const [keyword, setKeyword] = useState('')
  const [guides, setGuides] = useState<GuideCard[]>([])
  const [loading, setLoading] = useState(false)

  const loadList = async (menu: string, kw: string, s: 'new' | 'hot') => {
    setLoading(true)
    try {
      const params: GuidePageQuery = { page: 1, size: 20, sort: s }
      if (menu !== 'recommend') {
        if (CITY_KEYS.has(menu)) params.city = menu
        else params.tag = menu
      }
      if (kw.trim()) params.keyword = kw.trim()

      const res = await post<ApiResponse<PageResult<GuideCard>>>('/api/guide/page', params)
      if (res.code === 200) {
        setGuides(res.data.records)
      } else {
        message.error(res.message || '加载失败')
      }
    } catch {
      message.error('加载攻略失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadList(activeMenu, keyword, sort)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeMenu, sort])

  const onSearch = () => loadList(activeMenu, keyword, sort)

  const handlePublishMenu: MenuProps['onClick'] = ({ key }) => {
    if (key === 'pub-photo') navigate('/square/publish')
    else message.info('视频攻略开发中')
  }

  const todo = () => message.info('功能开发中，敬请期待')

  const userMenu: MenuProps['items'] = [
    { key: 'profile', label: '个人中心' },
    { type: 'divider' },
    { key: 'logout', label: '退出登录', onClick: onLogout },
  ]

  return (
    <div className="gs-page">
      {/* 顶部导航栏 */}
      <header className="gs-header">
        <div className="gs-logo" onClick={() => navigate('/')}>
          <span className="gs-logo-mark">
            <CompassOutlined />
          </span>
          <span className="gs-logo-text">小岛民攻略广场</span>
        </div>

        <nav className="gs-nav">
          <a className="gs-nav-item gs-nav-active" onClick={() => navigate('/')}>
            首页
          </a>
          <a className="gs-nav-item" onClick={todo}>
            行程灵感
          </a>
          <Dropdown menu={{ items: aiMenu, onClick: todo }} placement="bottom">
            <a className="gs-nav-item">
              AI生成行程 <DownOutlined className="gs-nav-arrow" />
            </a>
          </Dropdown>
          <Dropdown menu={{ items: tripMenu, onClick: todo }} placement="bottom">
            <a className="gs-nav-item">
              我的行程 <DownOutlined className="gs-nav-arrow" />
            </a>
          </Dropdown>
          <Dropdown menu={{ items: publishMenu, onClick: handlePublishMenu }} placement="bottom">
            <a className="gs-nav-item">
              发布攻略 <DownOutlined className="gs-nav-arrow" />
            </a>
          </Dropdown>
        </nav>

        <div className="gs-user">
          <Input
            className="gs-search"
            prefix={<SearchOutlined />}
            placeholder="搜索攻略"
            allowClear
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onPressEnter={onSearch}
          />
          <Dropdown menu={{ items: userMenu }} placement="bottomRight">
            <div className="gs-user-info">
              <Avatar className="gs-avatar">浓</Avatar>
              <span className="gs-nickname">啊浓</span>
            </div>
          </Dropdown>
        </div>
      </header>

      <div className="gs-body">
        {/* 左侧侧边栏 */}
        <aside className="gs-sidebar">
          <nav className="gs-side-menu">
            {sideMenu.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`gs-side-item ${activeMenu === item.key ? 'gs-side-active' : ''}`}
                onClick={() => setActiveMenu(item.key)}
              >
                {item.label}
              </button>
            ))}
          </nav>

          <div className="gs-side-tip">
            <img src={penguin} alt="海岛小企鹅" className="gs-tip-img" />
            <p className="gs-tip-text">AI生成行程后，可一键发布到攻略广场</p>
          </div>
        </aside>

        {/* 右侧主内容区 */}
        <main className="gs-main">
          <div className="gs-banner">
            <Carousel autoplay autoplaySpeed={4500}>
              {banners.map((b) => (
                <div key={b.key}>
                  <div className="gs-banner-slide" style={{ backgroundImage: `url(${b.img})` }}>
                    <div className="gs-banner-mask" />
                    <div className="gs-banner-content">
                      <h1 className="gs-banner-title">{b.title}</h1>
                      <p className="gs-banner-sub">{b.sub}</p>
                      <button
                        type="button"
                        className="gs-banner-btn"
                        onClick={() => navigate('/')}
                      >
                        开始规划行程
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </Carousel>
          </div>

          <div className="gs-content-grid">
            {/* 左侧瀑布流 */}
            <section className="gs-waterfall-wrap">
              <div className="gs-waterfall-head">
                <h2 className="gs-section-title">攻略卡片瀑布流</h2>
                <div className="gs-sort">
                  <button
                    type="button"
                    className={`gs-sort-btn ${sort === 'new' ? 'gs-sort-active' : ''}`}
                    onClick={() => setSort('new')}
                  >
                    最新
                  </button>
                  <button
                    type="button"
                    className={`gs-sort-btn ${sort === 'hot' ? 'gs-sort-active' : ''}`}
                    onClick={() => setSort('hot')}
                  >
                    最热
                  </button>
                </div>
              </div>

              {loading ? (
                <div className="gs-loading">
                  <Spin />
                </div>
              ) : guides.length === 0 ? (
                <Empty description="暂无攻略" style={{ marginTop: 60 }} />
              ) : (
                <div className="gs-waterfall">
                  {guides.map((g) => (
                    <article
                      key={g.id}
                      className="gs-card"
                      style={{ cursor: 'pointer' }}
                      onClick={() => navigate(`/square/detail/${g.id}`)}
                    >
                      <div className="gs-card-cover">
                        <img src={g.cover || fallbackCover} alt={g.title} loading="lazy" />
                      </div>
                      <div className="gs-card-body">
                        <div className="gs-card-tags">
                          <span className="gs-tag gs-tag-city">{g.city}</span>
                          <span className="gs-tag">{g.days}</span>
                          <span className="gs-tag">{g.budget}</span>
                        </div>
                        <h3 className="gs-card-title">{g.title}</h3>
                        <div className="gs-card-footer">
                          <div className="gs-card-author">
                            <span className="gs-card-avatar">
                              {g.authorName?.slice(0, 1) || '旅'}
                            </span>
                            <span className="gs-card-author-name">{g.authorName}</span>
                          </div>
                          <button
                            type="button"
                            className="gs-like"
                            onClick={(e) => {
                              e.stopPropagation()
                              message.info('点赞功能开发中')
                            }}
                          >
                            <HeartOutlined /> {formatCount(g.likes)}
                          </button>
                        </div>
                      </div>
                    </article>
                  ))}
                </div>
              )}
            </section>

            {/* 右侧热门路线 */}
            <aside className="gs-hot">
              <div className="gs-hot-card">
                <h3 className="gs-hot-title">
                  <FireOutlined className="gs-hot-fire" /> 本周热门路线
                </h3>
                <ul className="gs-hot-list">
                  {hotRoutes.map((r) => (
                    <li key={r.title} className="gs-hot-item">
                      <span className="gs-hot-icon" style={{ background: r.color }}>
                        {r.icon}
                      </span>
                      <div className="gs-hot-info">
                        <div className="gs-hot-name">{r.title}</div>
                        <div className="gs-hot-desc">{r.desc}</div>
                      </div>
                    </li>
                  ))}
                </ul>
                <button type="button" className="gs-hot-btn" onClick={todo}>
                  <PlusOutlined /> 查看全部路线
                </button>
              </div>

              <div className="gs-hot-tip">
                <img src={penguin} alt="海岛小企鹅" className="gs-hot-tip-img" />
                <p className="gs-hot-tip-text">一键收藏路线，下次出发不迷路～</p>
              </div>
            </aside>
          </div>
        </main>
      </div>
    </div>
  )
}

export default GuideSquare

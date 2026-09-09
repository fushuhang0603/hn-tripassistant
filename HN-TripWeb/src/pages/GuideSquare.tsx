import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Avatar, Carousel, Dropdown, Input } from 'antd'
import type { MenuProps } from 'antd'
import {
  CompassOutlined,
  DownOutlined,
  FireOutlined,
  HeartFilled,
  HeartOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import './GuideSquare.css'

interface Guide {
  id: number
  title: string
  cover: string
  city: string
  days: string
  budget: string
  tags: string[]
  author: string
  likes: number
}

const img = (prompt: string, size = 'landscape_16_9') =>
  `https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=${encodeURIComponent(
    prompt,
  )}&image_size=${size}`

const penguin = img(
  'cute cartoon penguin wearing sunglasses standing on tropical beach with coconut palm tree, 2D flat vector illustration, mint green and light blue color palette, clean white background, kawaii minimal',
  'square',
)

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

const guides: Guide[] = [
  {
    id: 1,
    title: '三亚4天3晚亲子游，带娃不累还出片',
    cover: img('Sanya Yalong Bay tropical beach turquoise sea coconut palms family travel'),
    city: '三亚',
    days: '4天3晚',
    budget: '¥8000',
    tags: ['亲子游', '海边度假'],
    author: '椰子妈妈',
    likes: 2341,
  },
  {
    id: 2,
    title: '海口骑楼老街一日漫游，南洋风情',
    cover: img('Haikou Qilou old street Nanyang style arcade buildings street food'),
    city: '海口',
    days: '2天1晚',
    budget: '¥1500',
    tags: ['美食探店', '人文'],
    author: '岛民阿东',
    likes: 1876,
  },
  {
    id: 3,
    title: '万宁冲浪新手入坑，石梅湾vs日月湾',
    cover: img('Wanning Shimei Bay surfing beach waves coconut road surfboard sunrise'),
    city: '万宁',
    days: '3天2晚',
    budget: '¥3500',
    tags: ['海边度假', '运动'],
    author: '浪里小白',
    likes: 2105,
  },
  {
    id: 4,
    title: '陵水分界洲岛潜水体验，第一次下海',
    cover: img('Lingshui Fenjiezhou Island glass sea diving underwater coral reef'),
    city: '陵水',
    days: '3天2晚',
    budget: '¥4000',
    tags: ['海边度假', '潜水'],
    author: '深海摄影师',
    likes: 1560,
  },
  {
    id: 5,
    title: '儋州千年古盐田 + 东坡书院人文慢旅',
    cover: img('Danzhou thousand year ancient salt field Dongpo academy historic culture'),
    city: '儋州',
    days: '2天1晚',
    budget: '¥1200',
    tags: ['人文', '亲子游'],
    author: '人文旅者',
    likes: 890,
  },
  {
    id: 6,
    title: '琼海博鳌深度游，除了会址还有什么',
    cover: img('Qionghai Boao seaside town Yudai beach fishing village laid-back'),
    city: '琼海',
    days: '2天1晚',
    budget: '¥1800',
    tags: ['海边度假', '美食探店'],
    author: '博鳌原住民',
    likes: 1120,
  },
  {
    id: 7,
    title: '三亚蜜月5日浪漫路线，不踩雷',
    cover: img('Hainan honeymoon romantic sunset beach couple luxury resort seaview'),
    city: '三亚',
    days: '5天4晚',
    budget: '¥12000',
    tags: ['海边度假', '蜜月'],
    author: '蜜月策划师',
    likes: 2760,
  },
  {
    id: 8,
    title: '海口周末美食探店，骑楼老街吃透',
    cover: img('Haikou street food night market local snacks qilou old street delicious'),
    city: '海口',
    days: '2天1晚',
    budget: '¥1000',
    tags: ['美食探店'],
    author: '吃货小椰',
    likes: 1980,
  },
  {
    id: 9,
    title: '万宁冲浪3天2晚，住进椰林海景房',
    cover: img('Wanning coconut forest sea view room resort beach sunset relaxation'),
    city: '万宁',
    days: '3天2晚',
    budget: '¥3200',
    tags: ['海边度假', '亲子游'],
    author: '椰林民宿主',
    likes: 1420,
  },
  {
    id: 10,
    title: '三亚海边度假3日，躺平治愈之旅',
    cover: img('Sanya beach resort infinity pool coconut tree sunny relaxation holiday'),
    city: '三亚',
    days: '3天2晚',
    budget: '¥6000',
    tags: ['海边度假', '亲子游'],
    author: '躺平旅行家',
    likes: 1690,
  },
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
  const [liked, setLiked] = useState<Set<number>>(new Set())

  const filtered = useMemo(() => {
    if (activeMenu === 'recommend') return guides
    return guides.filter((g) => g.city === activeMenu || g.tags.includes(activeMenu))
  }, [activeMenu])

  const toggleLike = (id: number) => {
    setLiked((prev) => {
      const next = new Set(prev)
      if (next.has(id)) {
        next.delete(id)
      } else {
        next.add(id)
        message.success('已点赞')
      }
      return next
    })
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
          <Dropdown menu={{ items: publishMenu, onClick: todo }} placement="bottom">
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
              <h2 className="gs-section-title">攻略卡片瀑布流</h2>
              <div className="gs-waterfall">
                {filtered.map((g) => (
                  <article key={g.id} className="gs-card">
                    <div className="gs-card-cover">
                      <img src={g.cover} alt={g.title} loading="lazy" />
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
                          <span className="gs-card-avatar">{g.author.slice(0, 1)}</span>
                          <span className="gs-card-author-name">{g.author}</span>
                        </div>
                        <button
                          type="button"
                          className={`gs-like ${liked.has(g.id) ? 'gs-liked' : ''}`}
                          onClick={() => toggleLike(g.id)}
                        >
                          {liked.has(g.id) ? <HeartFilled /> : <HeartOutlined />}{' '}
                          {formatCount(g.likes + (liked.has(g.id) ? 1 : 0))}
                        </button>
                      </div>
                    </div>
                  </article>
                ))}
              </div>
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

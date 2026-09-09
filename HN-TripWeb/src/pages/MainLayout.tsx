import { LogoutOutlined } from '@ant-design/icons'
import { NavLink, Outlet } from 'react-router-dom'
import './MainLayout.css'

interface MainLayoutProps {
  onLogout: () => void
}

function MainLayout({ onLogout }: MainLayoutProps) {
  return (
    <div className="app-shell">
      <div className="bg-grid" />

      <header className="header">
        <div className="header-left">
          <div className="logo-mark">椰</div>
          <h1 className="header-title">小岛民椰途</h1>
          <span className="sub-title">Hainan Smart Travel Intelligence System</span>
        </div>
        <nav className="header-actions">
          <NavLink
            to="/"
            end
            className={({ isActive }) => (isActive ? 'action-btn active' : 'action-btn')}
          >
            路线规划
          </NavLink>
          <NavLink
            to="/square"
            className={({ isActive }) => (isActive ? 'action-btn active' : 'action-btn')}
          >
            攻略广场
          </NavLink>
          <button className="action-btn" type="button">景点数据库</button>
          <button className="action-btn" type="button">出行报告</button>
          <button className="action-btn logout-btn" type="button" onClick={onLogout}>
            <LogoutOutlined /> 退出登录
          </button>
        </nav>
      </header>

      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}

export default MainLayout

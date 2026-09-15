import { useState } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import MainLayout from './pages/MainLayout'
import HomePage from './pages/HomePage'
import GuideSquare from './pages/GuideSquare'
import GuidePublish from './pages/GuidePublish'
import AmapMcpTest from './pages/AmapMcpTest'

function App() {
  const [authed, setAuthed] = useState<boolean>(() => !!localStorage.getItem('token'))

  const handleLogout = () => {
    localStorage.removeItem('token')
    setAuthed(false)
  }

  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            authed ? (
              <Navigate to="/" replace />
            ) : (
              <LoginPage onLoginSuccess={() => setAuthed(true)} />
            )
          }
        />

        <Route
          path="/"
          element={
            authed ? (
              <MainLayout onLogout={handleLogout} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        >
          <Route index element={<HomePage />} />
        </Route>

        <Route
          path="/square"
          element={
            authed ? (
              <GuideSquare onLogout={handleLogout} />
            ) : (
              <Navigate to="/login" replace />
            )
          }
        />

        <Route
          path="/square/publish"
          element={authed ? <GuidePublish /> : <Navigate to="/login" replace />}
        />

        <Route path="/amap-test" element={<AmapMcpTest />} />

        <Route path="*" element={<Navigate to={authed ? '/' : '/login'} replace />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App

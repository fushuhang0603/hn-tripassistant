import { useState } from 'react'
import { App, Button, Checkbox, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { post } from '../lib/request'
import type { ApiResponse, LoginParams, LoginResult, RegisterParams } from '../types/api'
import './LoginPage.css'

interface LoginForm {
  username: string
  password: string
  remember: boolean
}

interface RegisterForm {
  username: string
  password: string
  confirmPassword: string
}

const { Title, Text } = Typography

interface LoginPageProps {
  onLoginSuccess: () => void
}

function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const { message } = App.useApp()
  const [mode, setMode] = useState<'login' | 'register'>('login')

  const onLogin = async (values: LoginForm) => {
    try {
      const params: LoginParams = {
        username: values.username,
        password: values.password,
      }
      const data = await post<ApiResponse<LoginResult>>('/api/user/login', params)
      if (data.code === 200) {
        localStorage.setItem('token', data.data.token)
        message.success('登录成功')
        onLoginSuccess()
      } else {
        message.error(data.message || '登录失败')
      }
    } catch {
      message.error('网络错误，请稍后重试')
    }
  }

  const onRegister = async (values: RegisterForm) => {
    try {
      const params: RegisterParams = {
        username: values.username,
        password: values.password,
        confirmPassword: values.confirmPassword,
      }
      const data = await post<ApiResponse<null>>('/api/user/register', params)
      if (data.code === 200) {
        message.success('注册成功，请登录')
        setMode('login')
      } else {
        message.error(data.message || '注册失败')
      }
    } catch {
      message.error('网络错误，请稍后重试')
    }
  }

  return (
    <div className="login-page">
      <div className="login-banner">
        <div className="login-banner-overlay">
          <div className="login-banner-content">
            <Title className="login-title">小岛民椰途</Title>
            <Text className="login-slogan">碧海椰林，智能规划你的每一次出发</Text>
            <div className="login-tags">
              <span>景点推荐</span>
              <span>行程规划</span>
              <span>美食攻略</span>
            </div>
          </div>
        </div>
      </div>

      <div className="login-panel">
        <div className="login-box">
          {mode === 'login' ? (
            <>
              <Title level={3} className="login-box-title">欢迎登录</Title>
              <Text className="login-box-subtitle">大海与椰林，等你来探索</Text>

              <Form<LoginForm>
                name="login"
                size="large"
                initialValues={{ remember: true }}
                onFinish={onLogin}
                style={{ marginTop: 32 }}
              >
                <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                  <Input prefix={<UserOutlined />} placeholder="用户名" allowClear />
                </Form.Item>

                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>

                <Form.Item name="remember" valuePropName="checked" style={{ marginBottom: 16 }}>
                  <div className="login-options">
                    <Checkbox>记住我</Checkbox>
                    <a className="login-link" href="#forgot">忘记密码</a>
                  </div>
                </Form.Item>

                <Form.Item style={{ marginBottom: 16 }}>
                  <Button type="primary" htmlType="submit" block className="login-submit">
                    登 录
                  </Button>
                </Form.Item>
              </Form>

              <Text className="login-register-tip">
                还没有账号？<a className="login-link" onClick={() => setMode('register')}>立即注册</a>
              </Text>
            </>
          ) : (
            <>
              <Title level={3} className="login-box-title">创建账号</Title>
              <Text className="login-box-subtitle">注册后开启你的海南之旅</Text>

              <Form<RegisterForm>
                name="register"
                size="large"
                onFinish={onRegister}
                style={{ marginTop: 32 }}
              >
                <Form.Item name="username" rules={[{ required: true, message: '请输入用户名' }]}>
                  <Input prefix={<UserOutlined />} placeholder="用户名" allowClear />
                </Form.Item>

                <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                  <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: '请再次输入密码' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve()
                        }
                        return Promise.reject(new Error('两次输入的密码不一致'))
                      },
                    }),
                  ]}
                >
                  <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
                </Form.Item>

                <Form.Item style={{ marginBottom: 16 }}>
                  <Button type="primary" htmlType="submit" block className="login-submit">
                    注 册
                  </Button>
                </Form.Item>
              </Form>

              <Text className="login-register-tip">
                已有账号？<a className="login-link" onClick={() => setMode('login')}>去登录</a>
              </Text>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default LoginPage

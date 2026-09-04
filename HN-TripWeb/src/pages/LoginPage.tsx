import { App, Button, Checkbox, Divider, Form, Input, Typography } from 'antd'
import { LockOutlined, UserOutlined } from '@ant-design/icons'
import './LoginPage.css'

interface LoginForm {
  username: string
  password: string
  remember: boolean
}

const { Title, Text } = Typography

function LoginPage() {
  const { message } = App.useApp()

  const onFinish = (values: LoginForm) => {
    // TODO: 对接后端登录接口（如 POST /api/login），当前为演示逻辑
    console.log('登录参数:', values)
    message.success('登录成功（演示）')
  }

  return (
    <div className="login-page">
      {/* 左侧：海南主题品牌区 */}
      <div className="login-banner">
        <div className="login-banner-overlay">
          <div className="login-banner-content">
            <Title className="login-title">海南深度游AI助手</Title>
            <Text className="login-slogan">碧海椰林，智能规划你的每一次出发</Text>
            <div className="login-tags">
              <span>景点推荐</span>
              <span>行程规划</span>
              <span>美食攻略</span>
            </div>
          </div>
        </div>
      </div>

      {/* 右侧：登录表单区 */}
      <div className="login-panel">
        <div className="login-box">
          <Title level={3} className="login-box-title">欢迎登录</Title>
          <Text type="secondary">大海与椰林，等你来探索</Text>

          <Form<LoginForm>
            name="login"
            size="large"
            initialValues={{ remember: true }}
            onFinish={onFinish}
            style={{ marginTop: 32 }}
          >
            <Form.Item
              name="username"
              rules={[{ required: true, message: '请输入用户名' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="用户名" allowClear />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>

            <Form.Item name="remember" valuePropName="checked" style={{ marginBottom: 16 }}>
              <div className="login-options">
                <Checkbox>记住我</Checkbox>
                <a href="#forgot">忘记密码</a>
              </div>
            </Form.Item>

            <Form.Item style={{ marginBottom: 16 }}>
              <Button type="primary" htmlType="submit" block className="login-submit">
                登 录
              </Button>
            </Form.Item>
          </Form>

          <Divider plain>其他登录方式</Divider>
          <Text type="secondary" className="login-register-tip">
            还没有账号？<a href="#register">立即注册</a>
          </Text>
        </div>
      </div>
    </div>
  )
}

export default LoginPage

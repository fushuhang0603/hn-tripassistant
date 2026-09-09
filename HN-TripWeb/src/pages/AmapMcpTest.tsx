import { useState } from 'react'
import { App, Button, Card, Empty, Input, Space, Table, Tag, Typography } from 'antd'
import { PlayCircleOutlined, ReloadOutlined } from '@ant-design/icons'
import { get } from '../lib/request'

const { Title, Paragraph } = Typography

interface McpTool {
  name: string
  description: string
  inputSchema?: Record<string, unknown>
}

interface ToolsResult {
  clientCount: number
  toolCount: number
  tools: McpTool[]
}

interface CallResult {
  tool: string
  isError: boolean
  content: string[]
}

function AmapMcpTest() {
  const { message } = App.useApp()
  const [tools, setTools] = useState<McpTool[]>([])
  const [clientCount, setClientCount] = useState(0)
  const [loading, setLoading] = useState(false)
  const [selectedTool, setSelectedTool] = useState('')
  const [argsJson, setArgsJson] = useState('{"city":"三亚"}')
  const [callResult, setCallResult] = useState('')
  const [calling, setCalling] = useState(false)

  const loadTools = async () => {
    setLoading(true)
    try {
      const data = await get<ToolsResult>('/graph/amap/tools')
      setTools(data.tools ?? [])
      setClientCount(data.clientCount ?? 0)
      if (data.tools?.length) setSelectedTool(data.tools[0].name)
    } catch {
      message.error('加载工具列表失败，请确认后端已启动')
    } finally {
      setLoading(false)
    }
  }

  const doCall = async () => {
    if (!selectedTool) {
      message.warning('请先选择工具')
      return
    }
    setCalling(true)
    setCallResult('')
    try {
      const url = `/graph/amap/call?tool=${encodeURIComponent(selectedTool)}&args=${encodeURIComponent(argsJson)}`
      const data = await get<CallResult>(url)
      setCallResult(JSON.stringify(data, null, 2))
    } catch {
      message.error('调用失败')
    } finally {
      setCalling(false)
    }
  }

  const columns = [
    {
      title: '工具名',
      dataIndex: 'name',
      key: 'name',
      width: 220,
      render: (v: string) => <Tag color="cyan">{v}</Tag>,
    },
    {
      title: '描述',
      dataIndex: 'description',
      key: 'description',
    },
    {
      title: '操作',
      key: 'action',
      width: 90,
      render: (_: unknown, record: McpTool) => (
        <Button type="link" size="small" onClick={() => setSelectedTool(record.name)}>
          选择
        </Button>
      ),
    },
  ]

  return (
    <div style={{ maxWidth: 1000, margin: '0 auto', padding: 24 }}>
      <Title level={3}>高德地图 MCP 工具测试</Title>
      <Paragraph type="secondary">
        已连接 {clientCount} 个 MCP 客户端，共 {tools.length} 个工具。
      </Paragraph>

      <Card
        title="工具列表"
        extra={
          <Button icon={<ReloadOutlined />} onClick={loadTools} loading={loading}>
            刷新
          </Button>
        }
        style={{ marginBottom: 16 }}
      >
        {tools.length === 0 && !loading ? (
          <Empty description="暂无工具，点击右上角刷新" />
        ) : (
          <Table
            rowKey="name"
            columns={columns}
            dataSource={tools}
            loading={loading}
            pagination={false}
            size="small"
          />
        )}
      </Card>

      <Card title="调用工具">
        <Space direction="vertical" style={{ width: '100%' }} size="middle">
          <div>
            <div style={{ marginBottom: 8 }}>工具名：</div>
            <Input
              value={selectedTool}
              onChange={(e) => setSelectedTool(e.target.value)}
              placeholder="如 maps_weather"
            />
          </div>
          <div>
            <div style={{ marginBottom: 8 }}>参数 JSON：</div>
            <Input.TextArea
              value={argsJson}
              onChange={(e) => setArgsJson(e.target.value)}
              rows={4}
            />
          </div>
          <Button type="primary" icon={<PlayCircleOutlined />} onClick={doCall} loading={calling}>
            调用
          </Button>
        </Space>
      </Card>

      {callResult && (
        <Card title="调用结果" style={{ marginTop: 16 }}>
          <pre
            style={{
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
              background: '#f5f7fa',
              padding: 16,
              borderRadius: 8,
              margin: 0,
            }}
          >
            {callResult}
          </pre>
        </Card>
      )}
    </div>
  )
}

export default AmapMcpTest

import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { App, Button, Form, Input, Select, Spin, Upload } from 'antd'
import type { UploadFile } from 'antd'
import { ArrowLeftOutlined, PlusOutlined } from '@ant-design/icons'
import { post, uploadImages } from '../lib/request'
import type { ApiResponse, GuidePublishParams } from '../types/api'
import './GuidePublish.css'

const cityOptions = ['三亚', '万宁', '海口', '琼海', '陵水', '文昌'].map((c) => ({
  value: c,
  label: c,
}))

const dayOptions = ['1天', '2天1晚', '3天2晚', '4天3晚', '5天4晚', '6天5晚', '7天6晚'].map(
  (d) => ({ value: d, label: d }),
)

const tagOptions = ['亲子游', '海边度假', '美食探店', '冲浪', '骑行', '自驾', '蜜月', '小众秘境'].map(
  (t) => ({ value: t, label: t }),
)

// 后端 /api/upload/images 接收 files 数组，最多 9 张；封面前后端走同一通道，单张即可
function toFiles(list: UploadFile[]): File[] {
  return list
    .map((f) => f.originFileObj as File | undefined)
    .filter((f): f is File => Boolean(f))
}

function GuidePublishPage() {
  const { message } = App.useApp()
  const navigate = useNavigate()
  const [form] = Form.useForm<GuidePublishParams>()
  const [coverList, setCoverList] = useState<UploadFile[]>([])
  const [imageList, setImageList] = useState<UploadFile[]>([])
  const [submitting, setSubmitting] = useState(false)

  const onFinish = async (values: GuidePublishParams) => {
    setSubmitting(true)
    try {
      const coverFiles = toFiles(coverList)
      const imageFiles = toFiles(imageList)

      let coverUrl: string | undefined
      if (coverFiles.length > 0) {
        const [url] = await uploadImages(coverFiles)
        coverUrl = url
      }

      let imageUrls: string[] = []
      if (imageFiles.length > 0) {
        imageUrls = await uploadImages(imageFiles)
      }

      const params: GuidePublishParams = {
        title: values.title.trim(),
        cover: coverUrl,
        city: values.city,
        days: values.days,
        budget: values.budget,
        tags: values.tags ?? [],
        content: values.content,
        images: imageUrls,
        // 后端未对 source 兜底，手动发布需显式标记 USER，避免入库为 null
        source: 'USER',
      }

      const res = await post<ApiResponse<number>>('/api/guide/publish', params)
      if (res.code === 200) {
        message.success('攻略发布成功')
        navigate(`/square?detail=${res.data}`)
      } else {
        message.error(res.message || '发布失败')
      }
    } catch (err) {
      message.error(err instanceof Error ? err.message : '发布失败')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="gp-page">
      <header className="gp-topbar">
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate('/square')}>
          返回攻略广场
        </Button>
        <h1 className="gp-topbar-title">发布图文攻略</h1>
      </header>

      <div className="gp-container">
        <Form
          form={form}
          layout="vertical"
          onFinish={onFinish}
          initialValues={{ city: '三亚', days: '3天2晚', tags: [] }}
          requiredMark="optional"
        >
          <Form.Item
            name="title"
            label="标题"
            rules={[{ required: true, message: '请输入标题' }]}
          >
            <Input placeholder="给攻略起个吸引人的标题" maxLength={50} showCount />
          </Form.Item>

          <div className="gp-row">
            <Form.Item
              name="city"
              label="城市"
              className="gp-row-item"
              rules={[{ required: true, message: '请选择城市' }]}
            >
              <Select options={cityOptions} placeholder="选择城市" showSearch />
            </Form.Item>

            <Form.Item
              name="days"
              label="行程时长"
              className="gp-row-item"
              rules={[{ required: true, message: '请输入行程时长' }]}
            >
              <Select options={dayOptions} placeholder="如 3天2晚" showSearch />
            </Form.Item>

            <Form.Item
              name="budget"
              label="预算"
              className="gp-row-item"
              rules={[{ required: true, message: '请输入预算' }]}
            >
              <Input placeholder="如 ¥3000" />
            </Form.Item>
          </div>

          <Form.Item name="tags" label="标签">
            <Select mode="tags" options={tagOptions} placeholder="选择或输入标签，回车确认" />
          </Form.Item>

          <Form.Item label="封面图（可选，未上传将使用默认图）">
            <Upload
              listType="picture-card"
              fileList={coverList}
              maxCount={1}
              accept=".jpg,.jpeg,.png,.webp"
              beforeUpload={() => false}
              onChange={({ fileList }) => setCoverList(fileList)}
            >
              {coverList.length >= 1 ? null : (
                <div>
                  <PlusOutlined />
                  <div className="gp-upload-text">上传封面</div>
                </div>
              )}
            </Upload>
          </Form.Item>

          <Form.Item
            name="content"
            label="正文"
            rules={[{ required: true, message: '请输入攻略正文' }]}
          >
            <Input.TextArea rows={8} placeholder="分享你的行程安排、美食推荐、实用建议……" showCount />
          </Form.Item>

          <Form.Item label="图集（最多 9 张）">
            <Upload
              listType="picture-card"
              fileList={imageList}
              multiple
              maxCount={9}
              accept=".jpg,.jpeg,.png,.webp"
              beforeUpload={() => false}
              onChange={({ fileList }) => setImageList(fileList)}
            >
              {imageList.length >= 9 ? null : (
                <div>
                  <PlusOutlined />
                  <div className="gp-upload-text">上传图片</div>
                </div>
              )}
            </Upload>
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting} className="gp-submit">
              {submitting ? <Spin size="small" /> : '发布攻略'}
            </Button>
          </Form.Item>
        </Form>
      </div>
    </div>
  )
}

export default GuidePublishPage

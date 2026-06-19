import { Modal, message } from 'ant-design-vue'

type ConfirmOptions = {
  title: string
  content: string
  okText?: string
  cancelText?: string
  danger?: boolean
}

export const feedback = {
  success: (content: string) => message.success(content),
  warning: (content: string) => message.warning(content),
  error: (content: string) => message.error(content),
  info: (content: string) => message.info(content)
}

export const confirmAction = (options: ConfirmOptions) =>
  new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: options.title,
      content: options.content,
      okText: options.okText || '确认',
      cancelText: options.cancelText || '取消',
      okButtonProps: {
        danger: options.danger
      },
      centered: true,
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancelled'))
    })
  })

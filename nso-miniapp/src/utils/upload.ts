export interface UploadOptions {
  url: string
  filePath: string
  name?: string
}

export function uploadFile(options: UploadOptions): Promise<UniApp.UploadFileSuccessCallbackResult> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: options.url,
      filePath: options.filePath,
      name: options.name || 'file',
      success: resolve,
      fail: reject
    })
  })
}

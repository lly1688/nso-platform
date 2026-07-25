export function scanCode(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.scanCode({
      onlyFromCamera: true,
      success: (result) => resolve(result.result),
      fail: reject
    })
  })
}

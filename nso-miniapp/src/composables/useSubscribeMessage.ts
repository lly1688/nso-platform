export function useSubscribeMessage() {
  function requestSubscribe(templateIds: string[]): Promise<UniApp.RequestSubscribeMessageSuccessCallbackResult> {
    return new Promise((resolve, reject) => {
      uni.requestSubscribeMessage({
        tmplIds: templateIds,
        success: resolve,
        fail: reject
      })
    })
  }

  return {
    requestSubscribe
  }
}

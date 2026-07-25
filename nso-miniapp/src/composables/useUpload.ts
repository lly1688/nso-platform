import type { UploadOptions } from '../utils/upload'
import { uploadFile } from '../utils/upload'

export function useUpload() {
  return {
    upload: (options: UploadOptions) => uploadFile(options)
  }
}

export function required(value: unknown): boolean {
  return value !== null && value !== undefined && String(value).trim().length > 0
}

export function mobile(value: string): boolean {
  return /^1[3-9]\d{9}$/.test(value)
}

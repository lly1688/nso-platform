export function hasPermission(permissions: string[], code: string): boolean {
  return permissions.includes('*:*:*') || permissions.includes(code)
}

export function hasAnyPermission(permissions: string[], codes: string[]): boolean {
  return codes.some((code) => hasPermission(permissions, code))
}

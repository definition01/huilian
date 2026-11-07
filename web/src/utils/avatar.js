/**
 * 后端返回的头像地址为 /files/...，静态资源实际挂在 /api/files/... 下
 */
export function resolveAvatar(url) {
  if (!url) return ''
  return url.startsWith('/files/') ? `/api${url}` : url
}

import request from './request'

export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

export function register(data: { username: string; password: string; nickname: string }) {
  return request.post('/auth/register', data)
}

export function getUserList() {
  return request.get('/users')
}

export function updateUserRole(userId: string, role: string) {
  return request.put(`/users/${userId}/role`, { role })
}

export function disableUser(userId: string) {
  return request.put(`/users/${userId}/disable`)
}

export function changeMyPassword(oldPassword: string, newPassword: string) {
  return request.put('/auth/password', { oldPassword, newPassword })
}

export function resetUserPassword(userId: string, newPassword: string) {
  return request.put(`/users/${userId}/password`, { newPassword })
}

export function getSystemMembers(systemId: string) {
  return request.get(`/spaces/${systemId}/members`)
}

export function addSystemMember(systemId: string, userId: string, role: string) {
  return request.post(`/spaces/${systemId}/members`, { userId, role })
}

export function removeSystemMember(systemId: string, userId: string) {
  return request.delete(`/spaces/${systemId}/members/${userId}`)
}

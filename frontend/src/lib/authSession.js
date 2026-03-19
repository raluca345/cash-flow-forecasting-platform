const TOKEN_KEY = "token";

export function storeAuthSession(authResponse, rememberMe = true) {
  const storage = rememberMe ? localStorage : sessionStorage;
  const otherStorage = rememberMe ? sessionStorage : localStorage;

  otherStorage.removeItem(TOKEN_KEY);
  storage.setItem(TOKEN_KEY, authResponse.token);
}

export function getAuthToken() {
  return localStorage.getItem(TOKEN_KEY) ?? sessionStorage.getItem(TOKEN_KEY);
}

export function clearAuthSession() {
  localStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(TOKEN_KEY);
}

export function getStoredRole() {
  return getRoleFromClaims(getStoredTokenClaims());
}

export function getStoredName() {
  return getNameFromClaims(getStoredTokenClaims());
}

export function getStoredCompanyId() {
  return getCompanyIdFromClaims(getStoredTokenClaims());
}

export function getStoredTokenClaims() {
  return extractTokenClaims(getAuthToken());
}

export function getRoleFromClaims(claims) {
  return claims?.role ?? null;
}

export function getNameFromClaims(claims) {
  return claims?.name ?? null;
}

export function getCompanyIdFromClaims(claims) {
  return claims?.companyId ?? null;
}

export function extractTokenClaims(token) {
  if (!token) {
    return null;
  }

  const parts = token.split(".");
  if (parts.length < 2) {
    return null;
  }

  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const paddedBase64 = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
    const json = atob(paddedBase64);
    return JSON.parse(json);
  } catch {
    return null;
  }
}

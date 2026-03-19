import { create } from "zustand";
import {
  clearAuthSession,
  extractTokenClaims,
  getAuthToken,
  getCompanyIdFromClaims,
  getNameFromClaims,
  getRoleFromClaims,
  storeAuthSession,
} from "../lib/authSession";

const EMPTY_AUTH_STATE = {
  token: null,
  isAuthenticated: false,
  name: null,
  role: null,
  companyId: null,
};

function buildAuthState(token) {
  if (!token) {
    return EMPTY_AUTH_STATE;
  }

  const claims = extractTokenClaims(token);

  return {
    token,
    isAuthenticated: true,
    name: getNameFromClaims(claims),
    role: getRoleFromClaims(claims),
    companyId: getCompanyIdFromClaims(claims),
  };
}

function readStoredAuthState() {
  return buildAuthState(getAuthToken());
}

const useAuthStore = create((set) => ({
  ...readStoredAuthState(),

  setAuthSession(authResponse, rememberMe = true) {
    storeAuthSession(authResponse, rememberMe);
    set(buildAuthState(authResponse?.token ?? null));
  },

  clearAuth() {
    clearAuthSession();
    set(EMPTY_AUTH_STATE);
  },

  hydrateAuth() {
    set(readStoredAuthState());
  },
}));

export default useAuthStore;

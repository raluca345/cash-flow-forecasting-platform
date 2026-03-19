import { apiFetch } from "./client";
import { normalizeAuthApiError } from "../lib/authErrors";

export async function signup(payload) {
  try {
    return await apiFetch("/auth/signup", {
      method: "POST",
      body: payload,
    });
  } catch (error) {
    throw normalizeAuthApiError(error);
  }
}

export async function login(payload) {
  try {
    return await apiFetch("/auth/login", {
      method: "POST",
      body: payload,
    });
  } catch (error) {
    throw normalizeAuthApiError(error);
  }
}

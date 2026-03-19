import { getAuthToken } from "../lib/authSession";

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api/v1";

export async function apiFetch(relativePath, options = {}) {
  const token = getAuthToken();
  const { headers = {}, body, ...restOptions } = options;

  const shouldSerializeJsonBody =
    body != null &&
    typeof body === "object" &&
    !(body instanceof FormData) &&
    !(body instanceof URLSearchParams) &&
    !(body instanceof Blob) &&
    !(body instanceof ArrayBuffer);

  const response = await fetch(`${API_BASE_URL}${relativePath}`, {
    ...restOptions,
    body: shouldSerializeJsonBody ? JSON.stringify(body) : body,
    headers: {
      ...(shouldSerializeJsonBody ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    throw await buildApiError(response);
  }

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("Content-Type") ?? "";
  if (!contentType.includes("application/json")) {
    return response.text();
  }

  return response.json();
}

async function buildApiError(response) {
  const contentType = response.headers.get("Content-Type") ?? "";
  let payload = null;

  try {
    payload = contentType.includes("application/json")
      ? await response.json()
      : await response.text();
  } catch {
    payload = null;
  }

  const rawMessage =
    typeof payload === "string"
      ? payload
      : payload?.message ?? `Request failed with status ${response.status}`;

  const error = new Error(rawMessage);
  error.status = response.status;
  error.code = typeof payload === "object" ? payload?.error : null;
  error.rawMessage = rawMessage;
  error.fieldErrors =
    typeof payload === "object" && payload?.fieldErrors ? payload.fieldErrors : {};

  return error;
}

export async function apiFetch(relativePath, options = {}) {
  const token = localStorage.getItem("token");

  const res = await fetch(`http://localhost:8080/api/v1${relativePath}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: token ? `Bearer ${token}` : undefined,
      ...options.headers,
    },
  });

  if (!res.ok) {
    throw new Error("API request failed");
  }

  return res.json();
}

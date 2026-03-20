const FRIENDLY_MESSAGE_MAP = {
  "Validation failed": "Please fix the highlighted fields and try again",
  "Email already in use": "An account with this email address already exists",
  "Company invite code has expired":
    "This invite code has expired. Ask your company admin for a new one",
  "Invalid or expired company invite code":
    "That invite code is invalid or expired",
  "Bad credentials": "Email and password don't match",
  "Invalid credentials": "Email and password don't match",
  "Email and password don't match": "Email and password don't match",
  "Company context required":
    "You need to sign in again before continuing",
  "An unexpected error occurred. Please try again later.":
    "Something went wrong. Please try again in a moment",
};

const FRIENDLY_FIELD_MESSAGE_MAP = {
  "User name is required": "Enter your full name",
  "User name must be between 2 and 255 characters":
    "Your full name must be at least 2 characters long",
  "Email is required": "Enter your email address",
  "Email should be valid": "Enter a valid email address",
  "Email must be at most 100 characters": "Your email address is too long",
  "Password is required": "Enter your password",
  "Password must be between 8 and 16 characters":
    "Your password must be between 8 and 16 characters",
  "Company invite code is required": "Enter your invite code",
  "Invite code must be between 6 and 64 characters":
    "Your invite code must be between 6 and 64 characters",
};

export function normalizeAuthApiError(error) {
  const normalizedError = new Error(
    getFriendlyAuthErrorMessage(error?.rawMessage ?? error?.message, error?.status)
  );
  normalizedError.status = error?.status;
  normalizedError.code = error?.code;
  normalizedError.rawMessage = error?.rawMessage ?? error?.message;
  normalizedError.fieldErrors = getFriendlyFieldErrors(error?.fieldErrors);

  return normalizedError;
}

export function getFriendlyAuthErrorMessage(message, status) {
  if (isNetworkFailure(status, message)) {
    return "We couldn't reach the server. Check your internet connection and try again";
  }

  if (!message) {
    return "Something went wrong. Please try again";
  }

  if (status === 401) {
    return "Email and password don't match";
  }

  return FRIENDLY_MESSAGE_MAP[message] ?? normalizeMessage(message);
}

function getFriendlyFieldErrors(fieldErrors = {}) {
  return Object.fromEntries(
    Object.entries(fieldErrors).map(([field, message]) => [
      field,
      normalizeMessage(FRIENDLY_FIELD_MESSAGE_MAP[message] ?? message),
    ])
  );
}

function normalizeMessage(message) {
  if (!message) {
    return message;
  }

  return message.replace(/\.$/, "");
}

function isNetworkFailure(status, message) {
  if (status != null) {
    return false;
  }

  const normalized = String(message ?? "").toLowerCase();
  return (
    normalized.includes("networkerror") ||
    normalized.includes("failed to fetch") ||
    normalized.includes("load failed") ||
    normalized.includes("network request failed")
  );
}

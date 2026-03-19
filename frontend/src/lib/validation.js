export function isValidEmail(value) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

export function validateLoginForm(form) {
  const errors = {};
  const email = form.email.trim();

  if (!email) {
    errors.email = "Enter your email address";
  } else if (!isValidEmail(email)) {
    errors.email = "Enter a valid email address";
  }

  if (!form.password) {
    errors.password = "Enter your password";
  }

  return errors;
}

export function validateSignupForm(form) {
  const errors = {};
  const name = form.name.trim();
  const email = form.email.trim();
  const companyInviteCode = form.companyInviteCode.trim();

  if (!name) {
    errors.name = "Enter your full name";
  } else if (name.length < 2) {
    errors.name = "Your full name must be at least 2 characters long";
  }

  if (!email) {
    errors.email = "Enter your email address";
  } else if (!isValidEmail(email)) {
    errors.email = "Enter a valid email address";
  }

  if (!companyInviteCode) {
    errors.companyInviteCode = "Enter your invite code";
  }

  if (!form.password) {
    errors.password = "Enter your password";
  } else if (form.password.length < 8 || form.password.length > 16) {
    errors.password = "Your password must be between 8 and 16 characters";
  }

  if (!form.confirmPassword || form.password !== form.confirmPassword) {
    errors.confirmPassword = "Passwords must match";
  }

  return errors;
}

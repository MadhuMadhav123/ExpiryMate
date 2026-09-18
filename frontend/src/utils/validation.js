export const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const passwordRules = [
  { label: 'At least 8 characters', test: (value) => value.length >= 8 },
  { label: 'One uppercase letter', test: (value) => /[A-Z]/.test(value) },
  { label: 'One lowercase letter', test: (value) => /[a-z]/.test(value) },
  { label: 'One number', test: (value) => /\d/.test(value) },
  { label: 'One special character', test: (value) => /[^A-Za-z0-9]/.test(value) },
];

export function passwordStrength(password) {
  if (!password) {
    return { score: 0, label: 'Enter a password', className: 'secondary' };
  }

  const score = passwordRules.filter((rule) => rule.test(password)).length;

  if (score <= 2) {
    return { score, label: 'Weak', className: 'danger' };
  }

  if (score <= 4) {
    return { score, label: 'Medium', className: 'warning' };
  }

  return { score, label: 'Strong', className: 'success' };
}

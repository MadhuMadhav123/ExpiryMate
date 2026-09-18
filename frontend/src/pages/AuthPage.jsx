import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FileClock } from 'lucide-react';
import api from '../api';
import { emailRegex, passwordRules, passwordStrength } from '../utils/validation';

const initialForm = {
  name: '',
  email: '',
  password: '',
  confirmPassword: '',
};

function AuthPage({ mode, onAuthenticated }) {
  const navigate = useNavigate();
  const isRegister = mode === 'register';

  const [form, setForm] = useState(initialForm);
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setForm(initialForm);
    setErrors({});
    setServerError('');
    setShowPassword(false);
    setSubmitting(false);
  }, [mode]);

  const strength = useMemo(() => passwordStrength(form.password), [form.password]);

  function updateField(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setErrors((current) => ({ ...current, [name]: '' }));
    setServerError('');
  }

  function validate() {
    const nextErrors = {};
    const email = form.email.trim();

    if (isRegister && form.name.trim().length < 2) {
      nextErrors.name = 'Please enter your full name.';
    }

    if (!email) {
      nextErrors.email = 'Email is required.';
    } else if (!emailRegex.test(email)) {
      nextErrors.email = 'Please enter a valid email address.';
    }

    if (!form.password) {
      nextErrors.password = 'Password is required.';
    } else if (isRegister && passwordRules.some((rule) => !rule.test(form.password))) {
      nextErrors.password = 'Password does not meet all requirements.';
    }

    if (isRegister && !form.confirmPassword) {
      nextErrors.confirmPassword = 'Please confirm your password.';
    } else if (isRegister && form.confirmPassword !== form.password) {
      nextErrors.confirmPassword = 'Passwords do not match.';
    }

    setErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!validate()) {
      return;
    }

    setSubmitting(true);
    setServerError('');

    try {
      const payload = isRegister
        ? {
            name: form.name.trim(),
            email: form.email.trim(),
            password: form.password,
          }
        : {
            email: form.email.trim(),
            password: form.password,
          };

      const { data } = await api.post(`/api/auth/${mode}`, payload);
      onAuthenticated(data);
      navigate('/', { replace: true });
    } catch (error) {
      const backendErrors = error.response?.data?.errors;
      setErrors(backendErrors || {});

      if (!error.response) {
        setServerError('Cannot reach the API Gateway. Verify all services are running.');
      } else {
        setServerError(error.response.data?.message || 'Request failed. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-wrap">
      <div className="card auth-card shadow-lg border-0">
        <div className="card-body p-4 p-md-5">
          <div className="brand mb-4 d-flex align-items-center gap-2"><FileClock size={28} /> <span>ExpiryMate</span></div>
          <h3>{isRegister ? 'Create account' : 'Welcome back'}</h3>
          <p className="text-secondary">Never miss an important document renewal.</p>

          {serverError && <div className="alert alert-danger">{serverError}</div>}

          <form onSubmit={handleSubmit} noValidate>
            {isRegister && (
              <div className="mb-3">
                <label className="form-label" htmlFor="name">Full name</label>
                <input
                  id="name"
                  name="name"
                  className={`form-control ${errors.name ? 'is-invalid' : ''}`}
                  value={form.name}
                  onChange={updateField}
                  autoComplete="name"
                />
                {errors.name && <div className="invalid-feedback">{errors.name}</div>}
              </div>
            )}

            <div className="mb-3">
              <label className="form-label" htmlFor="email">Email address</label>
              <input
                id="email"
                name="email"
                type="email"
                className={`form-control ${errors.email ? 'is-invalid' : ''}`}
                value={form.email}
                onChange={updateField}
                autoComplete="email"
              />
              {errors.email && <div className="invalid-feedback">{errors.email}</div>}
            </div>

            <div className="mb-3">
              <label className="form-label" htmlFor="password">Password</label>
              <div className="input-group has-validation">
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  className={`form-control ${errors.password ? 'is-invalid' : ''}`}
                  value={form.password}
                  onChange={updateField}
                  autoComplete={isRegister ? 'new-password' : 'current-password'}
                />
                <button
                  type="button"
                  className="btn btn-outline-secondary"
                  onClick={() => setShowPassword((current) => !current)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
                {errors.password && <div className="invalid-feedback">{errors.password}</div>}
              </div>
            </div>

            {isRegister && (
              <>
                <div className="mb-3">
                  <div className="d-flex justify-content-between small mb-1">
                    <span>Password strength</span>
                    <strong className={`text-${strength.className}`}>{strength.label}</strong>
                  </div>
                  <div className="progress password-progress">
                    <div
                      className={`progress-bar bg-${strength.className}`}
                      style={{ width: `${(strength.score / passwordRules.length) * 100}%` }}
                    />
                  </div>
                  <ul className="password-rules mt-2 mb-0">
                    {passwordRules.map((rule) => {
                      const passed = rule.test(form.password);
                      return (
                        <li key={rule.label} className={passed ? 'text-success' : 'text-secondary'}>
                          {passed ? '✓' : '○'} {rule.label}
                        </li>
                      );
                    })}
                  </ul>
                </div>

                <div className="mb-3">
                  <label className="form-label" htmlFor="confirmPassword">Confirm password</label>
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type={showPassword ? 'text' : 'password'}
                    className={`form-control ${errors.confirmPassword ? 'is-invalid' : ''}`}
                    value={form.confirmPassword}
                    onChange={updateField}
                    autoComplete="new-password"
                  />
                  {errors.confirmPassword && (
                    <div className="invalid-feedback">{errors.confirmPassword}</div>
                  )}
                </div>
              </>
            )}

            <button className="btn btn-primary w-100" disabled={submitting}>
              {submitting ? 'Please wait...' : isRegister ? 'Register' : 'Login'}
            </button>
          </form>

          <div className="mt-3 small text-center">
            {isRegister ? (
              <>Already registered? <Link to="/login">Login</Link></>
            ) : (
              <>No account? <Link to="/register">Register</Link></>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

export default AuthPage;

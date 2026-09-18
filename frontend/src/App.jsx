import { useState } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';

function App() {
  const [token, setToken] = useState(() => localStorage.getItem('token'));

  function handleAuthenticated(authData) {
    localStorage.setItem('token', authData.token);
    localStorage.setItem('user', JSON.stringify(authData));
    setToken(authData.token);
  }

  function handleLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setToken(null);
  }

  return (
    <Routes>
      <Route
        path="/login"
        element={
          token ? (
            <Navigate to="/" replace />
          ) : (
            <AuthPage mode="login" onAuthenticated={handleAuthenticated} />
          )
        }
      />
      <Route
        path="/register"
        element={
          token ? (
            <Navigate to="/" replace />
          ) : (
            <AuthPage mode="register" onAuthenticated={handleAuthenticated} />
          )
        }
      />
      <Route
        path="/"
        element={
          token ? (
            <Dashboard onLogout={handleLogout} onAuthenticated={handleAuthenticated} />
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
      <Route path="*" element={<Navigate to={token ? '/' : '/login'} replace />} />
    </Routes>
  );
}

export default App;

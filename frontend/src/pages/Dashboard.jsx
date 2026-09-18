import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Bell,
  CheckCircle2,
  ChevronDown,
  CircleAlert,
  Clock3,
  Download,
  FileClock,
  FilePlus2,
  Files,
  FileText,
  LayoutDashboard,
  LogOut,
  Pencil,
  Plus,
  Trash2,
  UserRound,
} from 'lucide-react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import api from '../api';
import { passwordRules, passwordStrength } from '../utils/validation';

const categories = [
  'PASSPORT',
  'VISA',
  'DRIVING_LICENCE',
  'VEHICLE_INSURANCE',
  'IDENTIFICATION_CARD',
  'CERTIFICATE',
  'OTHER',
];

const categoryLabels = {
  PASSPORT: 'Passport',
  VISA: 'Visa',
  DRIVING_LICENCE: 'Driving Licence',
  VEHICLE_INSURANCE: 'Vehicle Insurance',
  IDENTIFICATION_CARD: 'ID Card',
  CERTIFICATE: 'Certificate',
  OTHER: 'Other',
};

const emptyDocument = {
  name: '',
  category: 'PASSPORT',
  documentNumber: '',
  issueDate: '',
  expiryDate: '',
  notes: '',
};

const emptyPasswordForm = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

const PIE_COLORS = ['#3f83f8', '#30b878', '#f6b73c', '#ef6461', '#8b5cf6', '#2db7c9', '#64748b'];

function formatCategory(category) {
  return categoryLabels[category] || category;
}

function formatDate(date) {
  if (!date) return '—';
  return new Intl.DateTimeFormat('en-US', {
    month: 'short',
    day: '2-digit',
    year: 'numeric',
  }).format(new Date(`${date}T00:00:00`));
}

function statusClass(status) {
  if (status === 'ACTIVE') return 'status-active';
  if (status === 'EXPIRING') return 'status-expiring';
  return 'status-expired';
}

function statusLabel(status) {
  if (status === 'EXPIRING') return 'Expiring Soon';
  if (status === 'ACTIVE') return 'Active';
  return 'Expired';
}

function monthKey(date) {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(date) {
  return new Intl.DateTimeFormat('en-US', { month: 'short', year: 'numeric' }).format(date);
}

function Dashboard({ onLogout, onAuthenticated }) {
  const navigate = useNavigate();
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || '{}'));
  const [documents, setDocuments] = useState([]);
  const [stats, setStats] = useState({ active: 0, expiring: 0, expired: 0, total: 0 });
  const [activeSection, setActiveSection] = useState('dashboard');
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [status, setStatus] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(emptyDocument);
  const [selectedFile, setSelectedFile] = useState(null);
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [saving, setSaving] = useState(false);

  const [profileName, setProfileName] = useState(user.name || '');
  const [passwordForm, setPasswordForm] = useState(emptyPasswordForm);
  const [profileError, setProfileError] = useState('');
  const [profileSuccess, setProfileSuccess] = useState('');
  const [showPasswords, setShowPasswords] = useState(false);

  const newPasswordStrength = useMemo(
    () => passwordStrength(passwordForm.newPassword),
    [passwordForm.newPassword],
  );

  async function loadDashboard() {
    const [documentsResponse, dashboardResponse] = await Promise.all([
      api.get('/api/documents'),
      api.get('/api/documents/dashboard'),
    ]);
    setDocuments(documentsResponse.data);
    setStats(dashboardResponse.data);
  }

  useEffect(() => {
    loadDashboard().catch((requestError) => {
      if (requestError.response?.status === 401 || requestError.response?.status === 403) {
        onLogout();
        navigate('/login', { replace: true });
      } else {
        setError('Unable to load your documents. Verify the API Gateway and Document Service.');
      }
    });
  }, []);

  const filteredDocuments = useMemo(() => {
    return documents.filter((document) => {
      const matchesSearch =
        !search ||
        document.name.toLowerCase().includes(search.toLowerCase()) ||
        (document.documentNumber || '').toLowerCase().includes(search.toLowerCase());
      const matchesCategory = !category || document.category === category;
      const matchesStatus = !status || document.status === status;
      return matchesSearch && matchesCategory && matchesStatus;
    });
  }, [documents, search, category, status]);

  const categoryData = useMemo(() => {
    return categories
      .map((item) => ({
        name: formatCategory(item),
        value: documents.filter((document) => document.category === item).length,
      }))
      .filter((item) => item.value > 0);
  }, [documents]);

  const statusData = useMemo(
    () => [
      { name: 'Active', count: stats.active },
      { name: 'Expiring Soon', count: stats.expiring },
      { name: 'Expired', count: stats.expired },
    ],
    [stats],
  );

  const upcomingExpirationData = useMemo(() => {
    const firstMonth = new Date();
    firstMonth.setDate(1);
    const months = Array.from({ length: 6 }, (_, index) => {
      const date = new Date(firstMonth.getFullYear(), firstMonth.getMonth() + index, 1);
      return { key: monthKey(date), month: monthLabel(date), count: 0 };
    });

    const monthMap = new Map(months.map((item) => [item.key, item]));
    documents.forEach((document) => {
      if (!document.expiryDate) return;
      const date = new Date(`${document.expiryDate}T00:00:00`);
      const target = monthMap.get(monthKey(date));
      if (target) target.count += 1;
    });
    return months;
  }, [documents]);

  const recentDocuments = useMemo(() => {
    return [...documents]
      .sort((a, b) => (b.id || 0) - (a.id || 0))
      .slice(0, 5);
  }, [documents]);

  const reminderDocuments = useMemo(() => {
    return documents.filter((document) => document.status !== 'ACTIVE');
  }, [documents]);

  function validateDocumentForm() {
    const nextErrors = {};
    const trimmedName = form.name.trim();
    const trimmedNumber = form.documentNumber.trim();

    if (trimmedName.length < 2) {
      nextErrors.name = 'Document name must contain at least 2 characters.';
    }
    if (!form.category) {
      nextErrors.category = 'Please select a category.';
    }
    if (!form.expiryDate) {
      nextErrors.expiryDate = 'Expiry date is required.';
    }
    if (form.issueDate && form.expiryDate && form.issueDate > form.expiryDate) {
      nextErrors.issueDate = 'Issue date cannot be after expiry date.';
    }

    const duplicateName = documents.some(
      (document) =>
        document.id !== editingId && document.name.trim().toLowerCase() === trimmedName.toLowerCase(),
    );
    if (trimmedName && duplicateName) {
      nextErrors.name = 'A document with this name already exists.';
    }

    if (trimmedNumber) {
      const duplicateNumber = documents.some(
        (document) =>
          document.id !== editingId &&
          document.status !== 'EXPIRED' &&
          (document.documentNumber || '').trim().toLowerCase() === trimmedNumber.toLowerCase(),
      );
      if (duplicateNumber) {
        nextErrors.documentNumber =
          'This document number is already used by an active or expiring document.';
      }
    }

    if (selectedFile) {
      const allowed = ['application/pdf', 'image/jpeg', 'image/png'];
      if (!allowed.includes(selectedFile.type)) {
        nextErrors.file = 'Only PDF, JPG and PNG files are allowed.';
      } else if (selectedFile.size > 10 * 1024 * 1024) {
        nextErrors.file = 'File size must not exceed 10 MB.';
      }
    }

    setFieldErrors(nextErrors);
    return Object.keys(nextErrors).length === 0;
  }

  function updateForm(event) {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: '' }));
    setError('');
    setSuccess('');
  }

  async function saveDocument(event) {
    event.preventDefault();
    if (!validateDocumentForm()) return;

    setSaving(true);
    setError('');
    setSuccess('');

    try {
      let savedDocument;

      if (editingId) {
        const response = await api.put(`/api/documents/${editingId}`, form);
        savedDocument = response.data;
      } else {
        const response = await api.post('/api/documents', form);
        savedDocument = response.data;
      }

      if (selectedFile) {
        const fileData = new FormData();
        fileData.append('file', selectedFile);
        await api.post(`/api/documents/${savedDocument.id}/file`, fileData);
      }

      const expiresToday = savedDocument.expiryDate === new Date().toISOString().slice(0, 10);
      setSuccess(
        expiresToday
          ? 'Document saved. An expiry reminder was sent/requested immediately.'
          : `Document ${editingId ? 'updated' : 'added'} successfully.`,
      );

      resetDocumentForm();
      await loadDashboard();
      setActiveSection('documents');
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to save the document.');
    } finally {
      setSaving(false);
    }
  }

  function resetDocumentForm() {
    setForm(emptyDocument);
    setSelectedFile(null);
    setEditingId(null);
    setFieldErrors({});
    const fileInput = document.getElementById('documentFile');
    if (fileInput) fileInput.value = '';
  }

  function editDocument(documentToEdit) {
    setEditingId(documentToEdit.id);
    setForm({
      name: documentToEdit.name,
      category: documentToEdit.category,
      documentNumber: documentToEdit.documentNumber || '',
      issueDate: documentToEdit.issueDate || '',
      expiryDate: documentToEdit.expiryDate,
      notes: documentToEdit.notes || '',
    });
    setSelectedFile(null);
    setFieldErrors({});
    setError('');
    setSuccess('');
    setActiveSection('add');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function deleteDocument(id) {
    if (!window.confirm('Delete this document and its uploaded file?')) return;

    try {
      await api.delete(`/api/documents/${id}`);
      setSuccess('Document deleted successfully.');
      await loadDashboard();
    } catch (requestError) {
      setError(requestError.response?.data?.message || 'Unable to delete document.');
    }
  }

  async function downloadFile(documentToDownload) {
    try {
      const response = await api.get(`/api/documents/${documentToDownload.id}/file`, {
        responseType: 'blob',
      });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = documentToDownload.fileName || 'document';
      document.body.appendChild(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(url);
    } catch {
      setError('Unable to download the uploaded file.');
    }
  }

  function logout() {
    onLogout();
    navigate('/login', { replace: true });
  }

  async function saveProfile(event) {
    event.preventDefault();
    setProfileError('');
    setProfileSuccess('');

    if (profileName.trim().length < 2) {
      setProfileError('Name must contain at least 2 characters.');
      return;
    }
    if (profileName.trim().length > 80) {
      setProfileError('Name must not exceed 80 characters.');
      return;
    }

    try {
      const { data } = await api.put('/api/profile', { name: profileName.trim() });
      onAuthenticated(data);
      setUser(data);
      setProfileName(data.name);
      setProfileSuccess('Profile name updated successfully.');
    } catch (requestError) {
      setProfileError(requestError.response?.data?.message || 'Unable to update profile.');
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    setProfileError('');
    setProfileSuccess('');

    if (!passwordForm.currentPassword) {
      setProfileError('Current password is required.');
      return;
    }
    if (passwordRules.some((rule) => !rule.test(passwordForm.newPassword))) {
      setProfileError('New password does not meet all requirements.');
      return;
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      setProfileError('New password and confirm password do not match.');
      return;
    }
    if (passwordForm.currentPassword === passwordForm.newPassword) {
      setProfileError('New password must be different from the current password.');
      return;
    }

    try {
      const { data } = await api.put('/api/profile/password', {
        currentPassword: passwordForm.currentPassword,
        newPassword: passwordForm.newPassword,
      });
      setPasswordForm(emptyPasswordForm);
      setProfileSuccess(data.message || 'Password changed successfully.');
    } catch (requestError) {
      setProfileError(requestError.response?.data?.message || 'Unable to change password.');
    }
  }

  const menuItems = [
    { key: 'dashboard', label: 'Dashboard', icon: LayoutDashboard },
    { key: 'documents', label: 'My Documents', icon: Files },
    { key: 'add', label: 'Add Document', icon: FilePlus2 },
    { key: 'reminders', label: 'Reminders', icon: Bell },
    { key: 'profile', label: 'Profile', icon: UserRound },
  ];

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-mark"><FileClock size={29} strokeWidth={2.1} /></div>
          <div>
            <div className="brand-title">ExpiryMate</div>
            <div className="brand-subtitle">Track Today, Stay Prepared</div>
          </div>
        </div>

        <nav className="sidebar-nav">
          {menuItems.map(({ key, label, icon: MenuIcon }) => (
            <button
              type="button"
              key={key}
              className={`sidebar-link ${activeSection === key ? 'active' : ''}`}
              onClick={() => {
                setActiveSection(key);
                setError('');
                setSuccess('');
                if (key !== 'add' && editingId) resetDocumentForm();
              }}
            >
              <MenuIcon className="menu-icon" size={22} strokeWidth={2} />
              <span>{label}</span>
            </button>
          ))}

          <button type="button" className="sidebar-link logout-link" onClick={logout}>
            <LogOut className="menu-icon" size={22} strokeWidth={2} />
            <span>Logout</span>
          </button>
        </nav>
      </aside>

      <section className="content-shell">
        <header className="topbar">
          <div className="mobile-brand">ExpiryMate</div>
          <div className="user-menu" onClick={() => setActiveSection('profile')} role="button" tabIndex="0">
            <div className="avatar">{(user.name || 'U').slice(0, 2).toUpperCase()}</div>
            <div className="user-meta">
              <strong>{user.name || 'User'}</strong>
              <span>{user.email || ''}</span>
            </div>
            <ChevronDown className="chevron" size={18} strokeWidth={2} />
          </div>
        </header>

        <main className="page-content">
          {error && <div className="alert alert-danger app-alert">{error}</div>}
          {success && <div className="alert alert-success app-alert">{success}</div>}

          {activeSection === 'dashboard' && (
            <>
              <div className="page-heading">
                <div>
                  <h1>Dashboard</h1>
                  <p>Welcome back, {user.name || 'User'}! 👋</p>
                </div>
                <div className="today-label">
                  {new Intl.DateTimeFormat('en-US', {
                    weekday: 'long',
                    month: 'long',
                    day: 'numeric',
                    year: 'numeric',
                  }).format(new Date())}
                </div>
              </div>

              <div className="stat-grid">
                <div className="metric-card total-card">
                  <div className="metric-icon"><FileText size={27} strokeWidth={2.1} /></div>
                  <div><strong>{stats.total}</strong><span>Total Documents</span><small>All your documents</small></div>
                </div>
                <div className="metric-card active-card">
                  <div className="metric-icon"><CheckCircle2 size={28} strokeWidth={2.2} /></div>
                  <div><strong>{stats.active}</strong><span>Active</span><small>Valid documents</small></div>
                </div>
                <div className="metric-card expiring-card">
                  <div className="metric-icon"><Clock3 size={28} strokeWidth={2.2} /></div>
                  <div><strong>{stats.expiring}</strong><span>Expiring Soon</span><small>Within 30 days</small></div>
                </div>
                <div className="metric-card expired-card">
                  <div className="metric-icon"><CircleAlert size={28} strokeWidth={2.2} /></div>
                  <div><strong>{stats.expired}</strong><span>Expired</span><small>Past expiry date</small></div>
                </div>
              </div>

              <div className="dashboard-grid">
                <div className="dashboard-card">
                  <h3>Document Status</h3>
                  <div className="chart-wrap">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={statusData} margin={{ top: 16, right: 12, left: -18, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8edf5" />
                        <XAxis dataKey="name" tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                        <Tooltip cursor={{ fill: '#f7f9fc' }} />
                        <Bar dataKey="count" radius={[7, 7, 0, 0]}>
                          <Cell fill="#30b878" />
                          <Cell fill="#f6b73c" />
                          <Cell fill="#ef6461" />
                        </Bar>
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="dashboard-card">
                  <h3>Documents by Category</h3>
                  {categoryData.length ? (
                    <div className="chart-wrap">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={categoryData}
                            dataKey="value"
                            nameKey="name"
                            cx="45%"
                            cy="50%"
                            outerRadius={88}
                            label={({ value }) => value}
                          >
                            {categoryData.map((entry, index) => (
                              <Cell key={entry.name} fill={PIE_COLORS[index % PIE_COLORS.length]} />
                            ))}
                          </Pie>
                          <Tooltip />
                          <Legend layout="vertical" verticalAlign="middle" align="right" />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                  ) : (
                    <div className="empty-chart">Add documents to see category distribution.</div>
                  )}
                </div>

                <div className="dashboard-card">
                  <h3>Upcoming Expirations (Next 6 Months)</h3>
                  <div className="chart-wrap">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={upcomingExpirationData} margin={{ top: 16, right: 12, left: -18, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#e8edf5" />
                        <XAxis dataKey="month" tick={{ fontSize: 11 }} axisLine={false} tickLine={false} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 12 }} axisLine={false} tickLine={false} />
                        <Tooltip cursor={{ fill: '#f7f9fc' }} />
                        <Bar dataKey="count" fill="#3f83f8" radius={[7, 7, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>

                <div className="dashboard-card recent-card">
                  <div className="card-heading-row">
                    <h3>Recent Documents</h3>
                    <button className="link-button" type="button" onClick={() => setActiveSection('documents')}>View All</button>
                  </div>
                  <div className="table-responsive">
                    <table className="table dashboard-table align-middle">
                      <thead><tr><th>Document Name</th><th>Category</th><th>Expiry Date</th><th>Status</th></tr></thead>
                      <tbody>
                        {recentDocuments.length === 0 ? (
                          <tr><td colSpan="4" className="text-center text-secondary py-4">No documents yet.</td></tr>
                        ) : recentDocuments.map((document) => (
                          <tr key={document.id}>
                            <td className="fw-semibold">{document.name}</td>
                            <td>{formatCategory(document.category)}</td>
                            <td>{formatDate(document.expiryDate)}</td>
                            <td><span className={`status-pill ${statusClass(document.status)}`}>{statusLabel(document.status)}</span></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </>
          )}

          {activeSection === 'documents' && (
            <section>
              <div className="page-heading">
                <div><h1>My Documents</h1><p>Search, filter, download, edit or remove your saved documents.</p></div>
                <button className="btn btn-primary" onClick={() => setActiveSection('add')}><Plus size={17} strokeWidth={2.3} /> Add Document</button>
              </div>

              <div className="dashboard-card">
                <div className="filters-row">
                  <input className="form-control" placeholder="Search name or number" value={search} onChange={(event) => setSearch(event.target.value)} />
                  <select className="form-select" value={category} onChange={(event) => setCategory(event.target.value)}>
                    <option value="">All categories</option>
                    {categories.map((item) => <option key={item} value={item}>{formatCategory(item)}</option>)}
                  </select>
                  <select className="form-select" value={status} onChange={(event) => setStatus(event.target.value)}>
                    <option value="">All statuses</option>
                    <option value="ACTIVE">Active</option>
                    <option value="EXPIRING">Expiring Soon</option>
                    <option value="EXPIRED">Expired</option>
                  </select>
                </div>

                <div className="table-responsive">
                  <table className="table align-middle document-table">
                    <thead><tr><th>Name</th><th>Category</th><th>Number</th><th>Expiry</th><th>Status</th><th>File</th><th>Actions</th></tr></thead>
                    <tbody>
                      {filteredDocuments.length === 0 ? (
                        <tr><td colSpan="7" className="text-center text-secondary py-5">No matching documents found.</td></tr>
                      ) : filteredDocuments.map((document) => (
                        <tr key={document.id}>
                          <td className="fw-semibold">{document.name}</td>
                          <td>{formatCategory(document.category)}</td>
                          <td>{document.documentNumber || '—'}</td>
                          <td>{formatDate(document.expiryDate)}</td>
                          <td><span className={`status-pill ${statusClass(document.status)}`}>{statusLabel(document.status)}</span></td>
                          <td>
                            {document.hasFile ? (
                              <button className="btn btn-sm btn-outline-primary icon-btn" onClick={() => downloadFile(document)}><Download size={15} /> Download</button>
                            ) : <span className="text-secondary">No file</span>}
                          </td>
                          <td>
                            <div className="d-flex gap-2">
                              <button className="btn btn-sm btn-outline-secondary icon-btn" onClick={() => editDocument(document)}><Pencil size={15} /> Edit</button>
                              <button className="btn btn-sm btn-outline-danger icon-btn" onClick={() => deleteDocument(document.id)}><Trash2 size={15} /> Delete</button>
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </section>
          )}

          {activeSection === 'add' && (
            <section>
              <div className="page-heading"><div><h1>{editingId ? 'Edit Document' : 'Add Document'}</h1><p>Keep the details accurate so reminders reach you at the right time.</p></div></div>
              <div className="form-card">
                <form onSubmit={saveDocument} noValidate>
                  <div className="row g-4">
                    <div className="col-md-6">
                      <label className="form-label">Document name *</label>
                      <input name="name" className={`form-control ${fieldErrors.name ? 'is-invalid' : ''}`} value={form.name} onChange={updateForm} maxLength="120" placeholder="e.g. US Passport" />
                      {fieldErrors.name && <div className="invalid-feedback">{fieldErrors.name}</div>}
                    </div>
                    <div className="col-md-6">
                      <label className="form-label">Category *</label>
                      <select name="category" className={`form-select ${fieldErrors.category ? 'is-invalid' : ''}`} value={form.category} onChange={updateForm}>
                        {categories.map((item) => <option key={item} value={item}>{formatCategory(item)}</option>)}
                      </select>
                      {fieldErrors.category && <div className="invalid-feedback">{fieldErrors.category}</div>}
                    </div>
                    <div className="col-md-6">
                      <label className="form-label">Document number</label>
                      <input name="documentNumber" className={`form-control ${fieldErrors.documentNumber ? 'is-invalid' : ''}`} value={form.documentNumber} onChange={updateForm} maxLength="100" placeholder="Enter unique document number" />
                      {fieldErrors.documentNumber && <div className="invalid-feedback">{fieldErrors.documentNumber}</div>}
                    </div>
                    <div className="col-md-3">
                      <label className="form-label">Issue date</label>
                      <input name="issueDate" className={`form-control ${fieldErrors.issueDate ? 'is-invalid' : ''}`} type="date" value={form.issueDate} onChange={updateForm} />
                      {fieldErrors.issueDate && <div className="invalid-feedback">{fieldErrors.issueDate}</div>}
                    </div>
                    <div className="col-md-3">
                      <label className="form-label">Expiry date *</label>
                      <input name="expiryDate" className={`form-control ${fieldErrors.expiryDate ? 'is-invalid' : ''}`} type="date" value={form.expiryDate} onChange={updateForm} />
                      {fieldErrors.expiryDate && <div className="invalid-feedback">{fieldErrors.expiryDate}</div>}
                    </div>
                    <div className="col-md-6">
                      <label className="form-label">Upload document</label>
                      <input id="documentFile" type="file" className={`form-control ${fieldErrors.file ? 'is-invalid' : ''}`} accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png" onChange={(event) => { setSelectedFile(event.target.files?.[0] || null); setFieldErrors((current) => ({ ...current, file: '' })); }} />
                      {fieldErrors.file ? <div className="invalid-feedback">{fieldErrors.file}</div> : <div className="form-text">PDF, JPG or PNG. Maximum 10 MB.</div>}
                    </div>
                    <div className="col-md-6">
                      <label className="form-label">Notes</label>
                      <textarea name="notes" className="form-control" value={form.notes} onChange={updateForm} maxLength="1000" rows="3" placeholder="Optional notes" />
                    </div>
                  </div>

                  <div className="form-actions">
                    <button className="btn btn-primary px-4" disabled={saving}>{saving ? 'Saving...' : editingId ? 'Update Document' : 'Add Document'}</button>
                    <button type="button" className="btn btn-light px-4" onClick={() => { resetDocumentForm(); setActiveSection('documents'); }}>Cancel</button>
                  </div>
                </form>
              </div>
            </section>
          )}

          {activeSection === 'reminders' && (
            <section>
              <div className="page-heading"><div><h1>Reminders</h1><p>Documents that are already expired or approaching expiration.</p></div></div>
              <div className="dashboard-card">
                {reminderDocuments.length === 0 ? (
                  <div className="empty-state"><div className="empty-icon"><CheckCircle2 size={29} strokeWidth={2.2} /></div><h3>You're all caught up</h3><p>No documents are expiring within the next 30 days.</p></div>
                ) : (
                  <div className="reminder-list">
                    {reminderDocuments.map((document) => (
                      <div className="reminder-item" key={document.id}>
                        <div><strong>{document.name}</strong><span>{formatCategory(document.category)} · Expires {formatDate(document.expiryDate)}</span></div>
                        <span className={`status-pill ${statusClass(document.status)}`}>{statusLabel(document.status)}</span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </section>
          )}

          {activeSection === 'profile' && (
            <section>
              <div className="page-heading"><div><h1>Profile</h1><p>Update your personal information and account password.</p></div></div>
              {profileError && <div className="alert alert-danger">{profileError}</div>}
              {profileSuccess && <div className="alert alert-success">{profileSuccess}</div>}

              <div className="profile-grid">
                <div className="form-card">
                  <h3>Personal Information</h3>
                  <form onSubmit={saveProfile}>
                    <label className="form-label">Name</label>
                    <input className="form-control mb-3" value={profileName} onChange={(event) => { setProfileName(event.target.value); setProfileError(''); }} maxLength="80" />
                    <label className="form-label">Email</label>
                    <input className="form-control mb-4" value={user.email || ''} disabled />
                    <button className="btn btn-primary">Update Name</button>
                  </form>
                </div>

                <div className="form-card">
                  <h3>Change Password</h3>
                  <form onSubmit={changePassword}>
                    <label className="form-label">Current password</label>
                    <input type={showPasswords ? 'text' : 'password'} className="form-control mb-3" value={passwordForm.currentPassword} onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))} />
                    <label className="form-label">New password</label>
                    <input type={showPasswords ? 'text' : 'password'} className="form-control mb-2" value={passwordForm.newPassword} onChange={(event) => setPasswordForm((current) => ({ ...current, newPassword: event.target.value }))} />
                    <div className="d-flex justify-content-between small mb-1"><span>Password strength</span><strong className={`text-${newPasswordStrength.className}`}>{newPasswordStrength.label}</strong></div>
                    <div className="progress password-progress mb-3"><div className={`progress-bar bg-${newPasswordStrength.className}`} style={{ width: `${(newPasswordStrength.score / passwordRules.length) * 100}%` }} /></div>
                    <label className="form-label">Confirm new password</label>
                    <input type={showPasswords ? 'text' : 'password'} className="form-control mb-3" value={passwordForm.confirmPassword} onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))} />
                    <div className="form-check mb-3"><input id="showProfilePasswords" type="checkbox" className="form-check-input" checked={showPasswords} onChange={(event) => setShowPasswords(event.target.checked)} /><label className="form-check-label" htmlFor="showProfilePasswords">Show passwords</label></div>
                    <button className="btn btn-outline-primary">Change Password</button>
                  </form>
                </div>
              </div>
            </section>
          )}
        </main>
      </section>
    </div>
  );
}

export default Dashboard;

import { Navigate, Route, Routes } from 'react-router-dom';
import { AppLayout } from './components/AppLayout/AppLayout';
import { ProtectedRoute } from './components/ProtectedRoute/ProtectedRoute';
import { AnalysisProgress } from './pages/AnalysisProgress/AnalysisProgress';
import { Dashboard } from './pages/Dashboard/Dashboard';
import { DocumentDetails } from './pages/DocumentDetails/DocumentDetails';
import { History } from './pages/History/History';
import { Login } from './pages/Login/Login';
import { Results } from './pages/Results/Results';
import { Upload } from './pages/Upload/Upload';

export default function App() {
  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<Login />} />

      {/* Protected — guarded by ProtectedRoute, laid out by AppLayout */}
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="upload" element={<Upload />} />
          <Route path="history" element={<History />} />
          <Route path="documents/:id" element={<DocumentDetails />} />
          <Route path="documents/:id/analysis" element={<AnalysisProgress />} />
          <Route path="documents/:id/results" element={<Results />} />
        </Route>
      </Route>

      {/* Fallback */}
      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}

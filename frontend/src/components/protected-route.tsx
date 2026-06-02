import { ReactNode } from "react";
import { useAuth } from "react-oidc-context";
import { Navigate, useLocation } from "react-router";
import Wordmark from "./wordmark";
import Ornament from "./ornament";

interface ProtectedRouteProperties {
  children: ReactNode;
}

const ProtectedRoute: React.FC<ProtectedRouteProperties> = ({ children }) => {
  const { isLoading, isAuthenticated } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
        <div className="flex flex-col items-center gap-6 text-center">
          <Wordmark to="/" />
          <div className="w-48">
            <Ornament />
          </div>
          <p className="eyebrow">One moment</p>
          <p className="font-display text-xl italic text-muted-foreground">
            Checking your invitation…
          </p>
        </div>
      </div>
    );
  }

  if (!isAuthenticated) {
    localStorage.setItem(
      "redirectPath",
      globalThis.location.pathname + globalThis.location.search,
    );
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return <>{children}</>;
};

export default ProtectedRoute;

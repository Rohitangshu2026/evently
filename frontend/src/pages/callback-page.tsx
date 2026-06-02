import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router";
import Wordmark from "@/components/wordmark";
import Ornament from "@/components/ornament";

const CallbackPage: React.FC = () => {
  const { isLoading, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isLoading) {
      return;
    }

    if (isAuthenticated) {
      const redirectPath = localStorage.getItem("redirectPath");
      if (redirectPath) {
        localStorage.removeItem("redirectPath");
        navigate(redirectPath);
      }
    }
  }, [isLoading, isAuthenticated, navigate]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
      <div className="flex flex-col items-center gap-8 text-center reveal">
        <Wordmark to="/" tagline />
        <div className="w-64">
          <Ornament />
        </div>
        <p className="eyebrow">{isLoading ? "Authenticating" : "Welcome"}</p>
        <p className="max-w-sm font-display text-2xl italic text-muted-foreground">
          {isLoading ? "Verifying your credentials…" : "Completing your entry…"}
        </p>
      </div>
    </div>
  );
};

export default CallbackPage;

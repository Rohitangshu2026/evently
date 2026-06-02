import { useEffect } from "react";
import { useAuth } from "react-oidc-context";
import Wordmark from "@/components/wordmark";
import Ornament from "@/components/ornament";

const LoginPage: React.FC = () => {
  const { isLoading, isAuthenticated, signinRedirect } = useAuth();

  useEffect(() => {
    if (isLoading) return;
    if (!isAuthenticated) signinRedirect();
  }, [isLoading, isAuthenticated, signinRedirect]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
      <div className="flex flex-col items-center gap-8 text-center reveal">
        <Wordmark to="/" tagline />
        <div className="w-64">
          <Ornament />
        </div>
        <p className="eyebrow">One moment</p>
        <p className="max-w-sm font-display text-2xl italic text-muted-foreground">
          Escorting you to a private door…
        </p>
        <span
          className="mt-2 h-px w-16 bg-gold"
          style={{ animation: "soft-float 2s ease-in-out infinite" }}
        />
      </div>
    </div>
  );
};

export default LoginPage;

import { useAuth } from "react-oidc-context";
import { useNavigate } from "react-router";
import Wordmark from "./wordmark";
import { Button } from "./ui/button";

interface PublicNavProps {
  dashboardPath?: string;
}

const todayLabel = () =>
  new Date().toLocaleDateString("en-GB", {
    weekday: "long",
    day: "2-digit",
    month: "long",
  });

const PublicNav: React.FC<PublicNavProps> = ({
  dashboardPath = "/dashboard",
}) => {
  const { isAuthenticated, signinRedirect, signoutRedirect } = useAuth();
  const navigate = useNavigate();

  return (
    <header className="border-b border-border/60 bg-background/70 backdrop-blur">
      {/* Thin upper strip */}
      <div className="border-b border-border/60 bg-cream/40">
        <div className="container mx-auto flex items-center justify-between px-6 py-1.5 text-[0.62rem] uppercase tracking-[0.34em] text-muted-foreground">
          <span className="hidden md:block">By appointment & invitation</span>
          <span className="font-display italic tracking-[0.18em] normal-case">
            {todayLabel()}
          </span>
          <span className="hidden md:block">Volume MMXXVI · No. 01</span>
        </div>
      </div>

      <div className="container mx-auto flex items-center justify-between px-6 py-5">
        <Wordmark to="/" />

        <nav className="hidden items-center gap-10 md:flex">
          <a
            href="/"
            className="link-underline text-[0.78rem] uppercase tracking-[0.24em] text-muted-foreground hover:text-foreground"
          >
            Discover
          </a>
          <a
            href="/organizers"
            className="link-underline text-[0.78rem] uppercase tracking-[0.24em] text-muted-foreground hover:text-foreground"
          >
            For Organizers
          </a>
        </nav>

        <div className="flex items-center gap-3">
          {isAuthenticated ? (
            <>
              <Button
                variant="ghost"
                className="h-9 cursor-pointer text-[0.74rem] uppercase tracking-[0.22em]"
                onClick={() => navigate(dashboardPath)}
              >
                Dashboard
              </Button>
              <Button
                className="h-9 cursor-pointer rounded-full bg-ink px-5 text-[0.74rem] uppercase tracking-[0.22em] text-primary-foreground hover:bg-ink/90"
                onClick={() => signoutRedirect()}
              >
                Sign out
              </Button>
            </>
          ) : (
            <Button
              className="h-9 cursor-pointer rounded-full bg-ink px-5 text-[0.74rem] uppercase tracking-[0.22em] text-primary-foreground hover:bg-ink/90"
              onClick={() => signinRedirect()}
            >
              Sign in
            </Button>
          )}
        </div>
      </div>
      <div className="gold-rule-shimmer" />
    </header>
  );
};

export default PublicNav;

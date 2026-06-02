import { useRoles } from "@/hooks/use-roles";
import { useEffect } from "react";
import { useNavigate } from "react-router";
import Wordmark from "@/components/wordmark";
import Ornament from "@/components/ornament";

const DashboardPage: React.FC = () => {
  const { isLoading, isOrganizer, isStaff } = useRoles();
  const navigate = useNavigate();

  useEffect(() => {
    if (isLoading) return;
    if (isOrganizer) navigate("/dashboard/events", { replace: true });
    else if (isStaff) navigate("/dashboard/validate-qr", { replace: true });
    else navigate("/dashboard/tickets", { replace: true });
  }, [isLoading, isOrganizer, isStaff, navigate]);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-6 text-foreground">
      <div className="flex flex-col items-center gap-6 text-center reveal">
        <Wordmark to="/" tagline />
        <div className="w-48">
          <Ornament />
        </div>
        <p className="eyebrow">Loading your studio</p>
        <p className="font-display text-2xl italic text-muted-foreground">
          A moment, please…
        </p>
      </div>
    </div>
  );
};

export default DashboardPage;

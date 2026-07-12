import { useAuth } from "@/lib/auth";
import { Avatar, AvatarFallback } from "./ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { LogOut } from "lucide-react";
import { useRoles } from "@/hooks/use-roles";
import { Link, useLocation } from "react-router";
import Wordmark from "./wordmark";
import { cn } from "@/lib/utils";

interface NavLinkProps {
  to: string;
  label: string;
  active?: boolean;
}

const NavLink: React.FC<NavLinkProps> = ({ to, label, active }) => (
  <Link
    to={to}
    className={cn(
      "link-underline text-[0.78rem] uppercase tracking-[0.24em] transition-colors",
      active ? "text-foreground" : "text-muted-foreground hover:text-foreground",
    )}
  >
    {label}
  </Link>
);

const todayLabel = () =>
  new Date().toLocaleDateString("en-GB", {
    weekday: "long",
    day: "2-digit",
    month: "long",
  });

const NavBar: React.FC = () => {
  const { user, signoutRedirect } = useAuth();
  const { isOrganizer, isStaff } = useRoles();
  const location = useLocation();
  const pathname = location.pathname;

  const initials =
    user?.profile?.preferred_username?.slice(0, 2).toUpperCase() ?? "AU";

  return (
    <header className="sticky top-0 z-30 border-b border-border/70 bg-background/85 backdrop-blur-md">
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

      <div className="container mx-auto flex items-center justify-between px-6 py-4">
        <Wordmark to="/" />

        <nav className="hidden items-center gap-10 md:flex">
          {isOrganizer && (
            <NavLink
              to="/dashboard/events"
              label="Events"
              active={pathname.startsWith("/dashboard/events")}
            />
          )}
          {isStaff && (
            <NavLink
              to="/dashboard/validate-qr"
              label="Validate"
              active={pathname.startsWith("/dashboard/validate-qr")}
            />
          )}
          <NavLink
            to="/dashboard/tickets"
            label="Tickets"
            active={pathname.startsWith("/dashboard/tickets")}
          />
          <NavLink to="/" label="Discover" active={pathname === "/"} />
        </nav>

        <DropdownMenu>
          <DropdownMenuTrigger className="rounded-full outline-none focus-visible:ring-2 focus-visible:ring-gold/50">
            <div className="relative">
              <span className="absolute -inset-0.5 rounded-full border border-gold/40" />
              <Avatar className="h-9 w-9 border border-border">
                <AvatarFallback className="bg-cream text-[0.72rem] font-medium tracking-[0.18em] text-ink">
                  {initials}
                </AvatarFallback>
              </Avatar>
            </div>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className="w-60 border-border bg-popover text-popover-foreground"
            align="end"
          >
            <DropdownMenuLabel className="font-normal">
              <p className="font-display text-base text-foreground">
                {user?.profile?.preferred_username ?? "Guest"}
              </p>
              <p className="text-xs text-muted-foreground">
                {user?.profile?.email}
              </p>
            </DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="cursor-pointer text-[0.78rem] uppercase tracking-[0.24em]"
              onClick={() => signoutRedirect()}
            >
              <LogOut className="h-4 w-4" />
              <span>Sign out</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
      <div className="gold-rule-shimmer" />
    </header>
  );
};

export default NavBar;

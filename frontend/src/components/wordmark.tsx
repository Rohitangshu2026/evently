import { Link } from "react-router";
import { cn } from "@/lib/utils";

interface WordmarkProps {
  to?: string;
  className?: string;
  tagline?: boolean;
}

const Wordmark: React.FC<WordmarkProps> = ({
  to = "/",
  className,
  tagline = false,
}) => {
  return (
    <Link to={to} className={cn("group inline-flex items-center gap-3", className)}>
      <span className="relative inline-flex h-9 w-9 items-center justify-center rounded-full border border-border bg-card">
        <span
          aria-hidden
          className="absolute inset-1 rounded-full border border-gold/60"
        />
        <span className="font-display text-[15px] leading-none text-ink">E</span>
      </span>
      <span className="flex flex-col leading-none">
        <span className="font-display text-[1.35rem] tracking-tight text-foreground">
          Evently
        </span>
        {tagline && (
          <span className="mt-1 text-[0.62rem] uppercase tracking-[0.32em] text-muted-foreground">
            Tickets · Est. MMXXV
          </span>
        )}
      </span>
    </Link>
  );
};

export default Wordmark;

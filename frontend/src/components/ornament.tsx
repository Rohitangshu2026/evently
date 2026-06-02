import { cn } from "@/lib/utils";

interface OrnamentProps {
  className?: string;
  /** "full" = line · diamond · line, "mark" = standalone diamond */
  variant?: "full" | "mark";
}

const Ornament: React.FC<OrnamentProps> = ({ className, variant = "full" }) => {
  if (variant === "mark") {
    return (
      <svg
        className={cn("inline-block text-gold", className)}
        viewBox="0 0 24 24"
        width="14"
        height="14"
        aria-hidden
      >
        <g fill="currentColor">
          <path d="M12 2 L14 12 L12 22 L10 12 Z" opacity="0.85" />
          <path d="M2 12 L12 10 L22 12 L12 14 Z" opacity="0.85" />
          <circle cx="12" cy="12" r="1.3" />
        </g>
      </svg>
    );
  }

  return (
    <div
      className={cn(
        "flex w-full items-center justify-center gap-4",
        className,
      )}
      aria-hidden
    >
      <span className="h-px max-w-[28%] flex-1 bg-gradient-to-r from-transparent via-gold/55 to-gold/55" />
      <svg viewBox="0 0 36 12" width="36" height="12" className="text-gold">
        <g fill="currentColor">
          <circle cx="6" cy="6" r="1" opacity="0.6" />
          <path d="M18 1 L22 6 L18 11 L14 6 Z" />
          <circle cx="30" cy="6" r="1" opacity="0.6" />
        </g>
      </svg>
      <span className="h-px max-w-[28%] flex-1 bg-gradient-to-l from-transparent via-gold/55 to-gold/55" />
    </div>
  );
};

export default Ornament;

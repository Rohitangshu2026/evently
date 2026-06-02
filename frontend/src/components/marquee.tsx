import { cn } from "@/lib/utils";

interface MarqueeProps {
  items: string[];
  className?: string;
  /** seconds for one full traversal */
  duration?: number;
}

const Marquee: React.FC<MarqueeProps> = ({
  items,
  className,
  duration = 60,
}) => {
  const doubled = [...items, ...items];
  return (
    <div
      className={cn(
        "relative w-full overflow-hidden border-y border-border bg-cream/40 py-4",
        className,
      )}
      aria-hidden
    >
      <div
        className="flex whitespace-nowrap will-change-transform"
        style={{
          animation: `marquee-x ${duration}s linear infinite`,
        }}
      >
        {doubled.map((item, i) => (
          <span
            key={`${item}-${i}`}
            className="mx-10 inline-flex items-center gap-10 font-display text-[0.95rem] tracking-[0.18em] text-muted-foreground"
          >
            <span className="italic">{item}</span>
            <svg
              viewBox="0 0 24 24"
              width="10"
              height="10"
              className="text-gold"
              aria-hidden
            >
              <path
                d="M12 2 L14 12 L12 22 L10 12 Z M2 12 L12 10 L22 12 L12 14 Z"
                fill="currentColor"
              />
            </svg>
          </span>
        ))}
      </div>
      {/* Edge fades */}
      <div className="pointer-events-none absolute inset-y-0 left-0 w-24 bg-gradient-to-r from-background to-transparent" />
      <div className="pointer-events-none absolute inset-y-0 right-0 w-24 bg-gradient-to-l from-background to-transparent" />
    </div>
  );
};

export default Marquee;

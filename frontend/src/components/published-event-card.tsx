import { PublishedEventSummary } from "@/domain/domain";
import { Calendar, MapPin } from "lucide-react";
import { format } from "date-fns";
import { Link } from "react-router";
import RandomEventImage from "./random-event-image";

interface PublishedEventCardProperties {
  publishedEvent: PublishedEventSummary;
  index?: number;
}

const PublishedEventCard: React.FC<PublishedEventCardProperties> = ({
  publishedEvent,
  index,
}) => {
  const dateLabel =
    publishedEvent.start && publishedEvent.end
      ? `${format(publishedEvent.start, "PP")} — ${format(publishedEvent.end, "PP")}`
      : "Dates to be announced";

  return (
    <Link
      to={`/events/${publishedEvent.id}`}
      className="group block focus:outline-none"
    >
      <article className="relative flex h-full flex-col overflow-hidden rounded-lg border border-border bg-card transition-all duration-500 hover:-translate-y-1 hover:border-gold/60 hover:shadow-[0_28px_60px_-30px_rgba(60,40,10,0.45)]">
        {/* Image */}
        <div className="relative aspect-[4/5] overflow-hidden">
          <RandomEventImage seed={publishedEvent.id} />

          {/* Featured chip */}
          <span className="absolute left-4 top-4 inline-flex items-center gap-2 rounded-full border border-gold/40 bg-background/85 px-3 py-1 text-[0.62rem] uppercase tracking-[0.28em] text-foreground backdrop-blur">
            <span className="h-1 w-1 rounded-full bg-gold" />
            {typeof index === "number"
              ? `№ ${String(index + 1).padStart(2, "0")}`
              : "Featured"}
          </span>

          {/* Gold corner brackets — visible on hover */}
          <span className="pointer-events-none absolute left-3 top-3 h-5 w-5 border-l border-t border-gold/0 opacity-0 transition-all duration-500 group-hover:left-2 group-hover:top-2 group-hover:border-gold/80 group-hover:opacity-100" />
          <span className="pointer-events-none absolute right-3 top-3 h-5 w-5 border-r border-t border-gold/0 opacity-0 transition-all duration-500 group-hover:right-2 group-hover:top-2 group-hover:border-gold/80 group-hover:opacity-100" />
          <span className="pointer-events-none absolute bottom-3 left-3 h-5 w-5 border-b border-l border-gold/0 opacity-0 transition-all duration-500 group-hover:bottom-2 group-hover:left-2 group-hover:border-gold/80 group-hover:opacity-100" />
          <span className="pointer-events-none absolute bottom-3 right-3 h-5 w-5 border-b border-r border-gold/0 opacity-0 transition-all duration-500 group-hover:bottom-2 group-hover:right-2 group-hover:border-gold/80 group-hover:opacity-100" />
        </div>

        {/* Caption */}
        <div className="flex flex-1 flex-col gap-4 p-6">
          <div className="flex items-start justify-between gap-3">
            <h3 className="font-display text-xl leading-tight text-foreground">
              {publishedEvent.name}
            </h3>
            <svg
              viewBox="0 0 24 24"
              width="10"
              height="10"
              className="mt-1.5 shrink-0 text-gold/80"
              aria-hidden
            >
              <path
                d="M12 2 L14 12 L12 22 L10 12 Z M2 12 L12 10 L22 12 L12 14 Z"
                fill="currentColor"
              />
            </svg>
          </div>

          <div className="gold-rule" />

          <dl className="space-y-2 text-sm text-muted-foreground">
            <div className="flex items-start gap-2">
              <MapPin className="mt-0.5 h-3.5 w-3.5 text-gold" />
              <dd className="leading-snug">{publishedEvent.venue}</dd>
            </div>
            <div className="flex items-start gap-2">
              <Calendar className="mt-0.5 h-3.5 w-3.5 text-gold" />
              <dd className="leading-snug">{dateLabel}</dd>
            </div>
          </dl>

          <div className="mt-auto flex items-center justify-between pt-2">
            <span className="eyebrow">Reserve</span>
            <span className="font-display text-sm italic tracking-[0.05em] text-foreground transition-transform group-hover:translate-x-0.5">
              View &rarr;
            </span>
          </div>
        </div>
      </article>
    </Link>
  );
};

export default PublishedEventCard;

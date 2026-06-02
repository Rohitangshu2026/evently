import RandomEventImage from "@/components/random-event-image";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  PublishedEventDetails,
  PublishedEventTicketTypeDetails,
} from "@/domain/domain";
import { getPublishedEvent } from "@/lib/api";
import { AlertCircle, MapPin } from "lucide-react";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";
import PublicNav from "@/components/public-nav";
import Ornament from "@/components/ornament";
import { cn } from "@/lib/utils";

const PublishedEventsPage: React.FC = () => {
  const { id } = useParams();
  const [error, setError] = useState<string | undefined>();
  const [publishedEvent, setPublishedEvent] = useState<
    PublishedEventDetails | undefined
  >();
  const [selectedTicketType, setSelectedTicketType] = useState<
    PublishedEventTicketTypeDetails | undefined
  >();

  useEffect(() => {
    if (!id) {
      setError("ID must be provided!");
      return;
    }
    const doUseEffect = async () => {
      try {
        const eventData = await getPublishedEvent(id);
        setPublishedEvent(eventData);
        if (eventData.ticketTypes.length > 0) {
          setSelectedTicketType(eventData.ticketTypes[0]);
        }
      } catch (err) {
        if (err instanceof Error) setError(err.message);
        else if (typeof err === "string") setError(err);
        else setError("An unknown error has occurred");
      }
    };
    doUseEffect();
  }, [id]);

  if (error) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <PublicNav />
        <div className="container mx-auto px-6 py-16">
          <Alert variant="destructive" className="border-destructive/40 bg-card">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Error</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <PublicNav />

      <main className="container mx-auto px-6 py-16">
        {/* Header */}
        <div className="grid grid-cols-1 gap-12 md:grid-cols-12 md:items-end">
          <div className="reveal md:col-span-7">
            <p className="eyebrow">Featured engagement</p>
            <div className="mt-4">
              <Ornament variant="mark" />
            </div>
            <h1 className="mt-5 font-display text-5xl leading-[1.04] text-foreground md:text-6xl">
              {publishedEvent?.name}
            </h1>
            <div className="mt-8 flex items-center gap-3 text-sm text-muted-foreground">
              <MapPin className="h-4 w-4 text-gold" />
              <span className="tracking-wide">{publishedEvent?.venue}</span>
            </div>
          </div>
          <div className="reveal reveal-delay-1 md:col-span-5">
            <div className="relative aspect-[4/5] overflow-hidden rounded-lg border border-border bg-card shadow-[0_30px_60px_-30px_rgba(60,40,10,0.45)]">
              <RandomEventImage seed={publishedEvent?.id} />
              <span className="pointer-events-none absolute left-3 top-3 h-5 w-5 border-l border-t border-gold/70" />
              <span className="pointer-events-none absolute right-3 top-3 h-5 w-5 border-r border-t border-gold/70" />
              <span className="pointer-events-none absolute bottom-3 left-3 h-5 w-5 border-b border-l border-gold/70" />
              <span className="pointer-events-none absolute bottom-3 right-3 h-5 w-5 border-b border-r border-gold/70" />
            </div>
          </div>
        </div>

        <div className="my-16">
          <Ornament />
        </div>

        {/* Tickets */}
        <div className="grid grid-cols-1 gap-10 md:grid-cols-12">
          <div className="md:col-span-7">
            <div className="mb-8 flex items-baseline justify-between">
              <h2 className="font-display text-3xl text-foreground">
                Select an admission
              </h2>
              <span className="eyebrow">
                {publishedEvent?.ticketTypes?.length ?? 0} options
              </span>
            </div>

            <div className="space-y-3">
              {publishedEvent?.ticketTypes?.map((ticketType, idx) => {
                const isSelected = selectedTicketType?.id === ticketType.id;
                return (
                  <button
                    key={ticketType.id}
                    onClick={() => setSelectedTicketType(ticketType)}
                    className={cn(
                      "reveal group w-full cursor-pointer rounded-lg border bg-card px-6 py-5 text-left transition-all",
                      isSelected
                        ? "border-gold shadow-[0_22px_44px_-26px_rgba(60,40,10,0.45)]"
                        : "border-border hover:border-gold/50",
                    )}
                    style={{ animationDelay: `${idx * 80}ms` }}
                  >
                    <div className="flex items-start justify-between gap-6">
                      <div className="flex items-start gap-4">
                        <span
                          className={cn(
                            "mt-1 font-display text-xs italic tracking-[0.18em] transition-colors",
                            isSelected ? "text-gold" : "text-muted-foreground",
                          )}
                        >
                          № {String(idx + 1).padStart(2, "0")}
                        </span>
                        <div>
                          <h3 className="font-display text-xl text-foreground">
                            {ticketType.name}
                          </h3>
                          <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">
                            {ticketType.description}
                          </p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p className="eyebrow">Per guest</p>
                        <p className="mt-1 font-display text-2xl text-foreground">
                          ${ticketType.price}
                        </p>
                      </div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Summary */}
          <aside className="md:col-span-5">
            <div className="reveal reveal-delay-1 sticky top-28 rounded-lg border border-border bg-card p-8 shadow-[0_30px_60px_-30px_rgba(60,40,10,0.4)]">
              <div className="flex items-center justify-between">
                <p className="eyebrow">Reservation</p>
                <Ornament variant="mark" className="opacity-80" />
              </div>
              <h3 className="mt-3 font-display text-3xl text-foreground">
                {selectedTicketType?.name}
              </h3>
              <div className="mt-6 flex items-baseline gap-2">
                <span className="font-display text-5xl text-foreground">
                  ${selectedTicketType?.price}
                </span>
                <span className="text-sm text-muted-foreground">/ guest</span>
              </div>
              <p className="mt-6 text-sm leading-relaxed text-muted-foreground">
                {selectedTicketType?.description}
              </p>

              <div className="my-8">
                <Ornament />
              </div>

              <Link
                to={`/events/${publishedEvent?.id}/purchase/${selectedTicketType?.id}`}
              >
                <Button className="h-12 w-full cursor-pointer rounded-full bg-ink text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90">
                  Reserve admission
                </Button>
              </Link>
              <p className="mt-4 text-center text-[0.68rem] uppercase tracking-[0.28em] text-muted-foreground">
                Secure checkout · Instant QR
              </p>
            </div>
          </aside>
        </div>
      </main>
    </div>
  );
};

export default PublishedEventsPage;

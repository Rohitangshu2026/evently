import NavBar from "@/components/nav-bar";
import { formatInr } from "@/lib/currency";
import Ornament from "@/components/ornament";
import { TicketDetails, TicketStatus } from "@/domain/domain";
import { getTicket, getTicketQr } from "@/lib/api";
import { format } from "date-fns";
import { Calendar, MapPin } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useParams } from "react-router";
import { cn } from "@/lib/utils";

const DashboardViewTicketPage: React.FC = () => {
  const [ticket, setTicket] = useState<TicketDetails | undefined>();
  const [qrCodeUrl, setQrCodeUrl] = useState<string | undefined>();
  const [isQrLoading, setIsQrCodeLoading] = useState(true);
  const [error, setError] = useState<string | undefined>();

  const { id } = useParams();
  const { isLoading, user } = useAuth();

  useEffect(() => {
    if (isLoading || !user?.access_token || !id) return;

    const doUseEffect = async (accessToken: string, id: string) => {
      try {
        setIsQrCodeLoading(true);
        setError(undefined);
        setTicket(await getTicket(accessToken, id));
        setQrCodeUrl(URL.createObjectURL(await getTicketQr(accessToken, id)));
      } catch (err) {
        if (err instanceof Error) setError(err.message);
        else if (typeof err === "string") setError(err);
        else setError("An unknown error has occurred");
      } finally {
        setIsQrCodeLoading(false);
      }
    };

    doUseEffect(user?.access_token, id);

    return () => {
      if (qrCodeUrl) URL.revokeObjectURL(qrCodeUrl);
    };
  }, [user?.access_token, isLoading, id]);

  const getStatusColor = (status: TicketStatus) => {
    switch (status) {
      case TicketStatus.PURCHASED:
        return "border-gold/50 text-gold";
      case TicketStatus.CANCELLED:
        return "border-destructive/40 text-destructive";
      default:
        return "border-border text-muted-foreground";
    }
  };

  if (!ticket) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <NavBar />
        <div className="container mx-auto px-6 py-32 text-center">
          <Ornament variant="mark" />
          <p className="eyebrow mt-6">One moment</p>
          <p className="mt-4 font-display text-3xl italic text-muted-foreground">
            Drawing your admission…
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />

      <main className="container mx-auto flex items-center justify-center px-6 py-16">
        <div className="relative w-full max-w-md reveal">
          {/* Soft halo */}
          <div className="absolute -inset-6 -z-10 rounded-[28px] bg-gradient-to-br from-gold/15 via-transparent to-gold/10 blur-2xl" />

          {/* Ticket stub */}
          <article className="relative overflow-hidden rounded-lg border border-border bg-card shadow-[0_50px_100px_-50px_rgba(60,40,10,0.55)]">
            {/* Embossed watermark */}
            <div
              className="pointer-events-none absolute inset-0 flex items-center justify-center"
              aria-hidden
            >
              <span className="font-display text-[7.5rem] font-light uppercase tracking-[0.05em] engrave -rotate-[18deg] select-none">
                Admit
              </span>
            </div>

            {/* Header band */}
            <div className="relative px-8 pt-10 pb-8">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <Ornament variant="mark" />
                  <p className="eyebrow">Admission</p>
                </div>
                <span
                  className={cn(
                    "inline-flex rounded-full border bg-background/80 px-3 py-1 text-[0.6rem] uppercase tracking-[0.28em] backdrop-blur",
                    getStatusColor(ticket.status),
                  )}
                >
                  {ticket?.status}
                </span>
              </div>

              <h1 className="mt-6 font-display text-3xl leading-tight text-foreground">
                {ticket.eventName}
              </h1>

              <div className="mt-6 space-y-3 text-sm text-muted-foreground">
                <div className="flex items-center gap-3">
                  <MapPin className="h-4 w-4 text-gold" />
                  <span>{ticket.eventVenue}</span>
                </div>
                <div className="flex items-center gap-3">
                  <Calendar className="h-4 w-4 text-gold" />
                  <span>
                    {ticket.eventStart && ticket.eventEnd
                      ? `${format(ticket.eventStart, "Pp")} — ${format(ticket.eventEnd, "Pp")}`
                      : "Dates to be announced"}
                  </span>
                </div>
              </div>
            </div>

            {/* Perforation */}
            <div className="relative flex items-center">
              <span className="absolute -left-3 z-10 h-6 w-6 rounded-full bg-background" />
              <span className="absolute -right-3 z-10 h-6 w-6 rounded-full bg-background" />
              <div className="flex w-full items-center px-4">
                <span className="h-px flex-1 [background-image:repeating-linear-gradient(90deg,var(--border)_0_6px,transparent_6px_12px)]" />
              </div>
            </div>

            {/* QR section */}
            <div className="relative px-8 pt-8 pb-10">
              <div className="relative mx-auto h-48 w-48">
                {/* Decorative frame */}
                <span className="pointer-events-none absolute -inset-2 rounded-lg border border-gold/30" />
                <span className="pointer-events-none absolute -left-1 -top-1 h-4 w-4 border-l border-t border-gold" />
                <span className="pointer-events-none absolute -right-1 -top-1 h-4 w-4 border-r border-t border-gold" />
                <span className="pointer-events-none absolute -bottom-1 -left-1 h-4 w-4 border-b border-l border-gold" />
                <span className="pointer-events-none absolute -bottom-1 -right-1 h-4 w-4 border-b border-r border-gold" />

                <div className="flex h-full w-full items-center justify-center rounded-lg border border-border bg-background p-3">
                  {isQrLoading && (
                    <div className="text-center">
                      <div className="mx-auto mb-3 h-8 w-8 animate-spin rounded-full border-2 border-border border-t-gold" />
                      <p className="text-[0.66rem] uppercase tracking-[0.22em] text-muted-foreground">
                        Loading
                      </p>
                    </div>
                  )}
                  {error && (
                    <div className="text-center text-xs text-destructive">
                      {error}
                    </div>
                  )}
                  {qrCodeUrl && !isQrLoading && !error && (
                    <img
                      src={qrCodeUrl}
                      alt="QR Code for event entry"
                      className="h-full w-full object-contain"
                    />
                  )}
                </div>
              </div>

              <p className="mt-8 text-center text-[0.68rem] uppercase tracking-[0.28em] text-muted-foreground">
                Present at the door for entry
              </p>

              <div className="my-8">
                <Ornament />
              </div>

              <dl className="grid grid-cols-2 gap-4 text-sm">
                <div>
                  <dt className="eyebrow">Tier</dt>
                  <dd className="mt-1 font-display text-base text-foreground">
                    {ticket.description}
                  </dd>
                </div>
                <div className="text-right">
                  <dt className="eyebrow">Paid</dt>
                  <dd className="mt-1 font-display text-base text-foreground">
                    {formatInr(ticket.price)}
                  </dd>
                </div>
              </dl>

              <div className="mt-8 text-center">
                <p className="eyebrow">Reference</p>
                <p className="mt-1 break-all font-mono text-[0.7rem] text-muted-foreground">
                  {ticket.id}
                </p>
              </div>
            </div>
          </article>

          <p className="mt-8 text-center font-display text-sm italic text-muted-foreground">
            One ticket. One evening. Kept with care.
          </p>
        </div>
      </main>
    </div>
  );
};

export default DashboardViewTicketPage;

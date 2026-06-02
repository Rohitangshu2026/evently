import NavBar from "@/components/nav-bar";
import { SimplePagination } from "@/components/simple-pagination";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { SpringBootPagination, TicketSummary } from "@/domain/domain";
import { listTickets } from "@/lib/api";
import { AlertCircle, ArrowUpRight, Ticket } from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { Link } from "react-router";
import Ornament from "@/components/ornament";

const DashboardListTickets: React.FC = () => {
  const { isLoading, user } = useAuth();

  const [tickets, setTickets] = useState<
    SpringBootPagination<TicketSummary> | undefined
  >();
  const [error, setError] = useState<string | undefined>();
  const [page, setPage] = useState(0);

  useEffect(() => {
    if (isLoading || !user?.access_token) return;

    const doUseEffect = async () => {
      try {
        setTickets(await listTickets(user.access_token, page));
      } catch (err) {
        if (err instanceof Error) setError(err.message);
        else if (typeof err === "string") setError(err);
        else setError("An unknown error occurred");
      }
    };

    doUseEffect();
  }, [isLoading, user?.access_token, page]);

  if (error) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <NavBar />
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
      <NavBar />

      <main className="container mx-auto max-w-3xl px-6 py-16">
        <div className="reveal border-b border-border pb-10">
          <p className="eyebrow">Your portfolio</p>
          <div className="mt-3">
            <Ornament variant="mark" />
          </div>
          <h1 className="mt-3 font-display text-5xl text-foreground">
            Tickets in hand
          </h1>
          <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
            Every admission you've reserved. Tap to reveal the QR for entry.
          </p>
        </div>

        <div className="mt-10 space-y-4">
          {tickets?.content.map((ticketItem, idx) => (
            <Link
              key={ticketItem.id}
              to={`/dashboard/tickets/${ticketItem.id}`}
              className="reveal group block rounded-lg border border-border bg-card p-6 transition-all hover:border-gold/50 hover:-translate-y-0.5 hover:shadow-[0_22px_44px_-26px_rgba(60,40,10,0.4)]"
              style={{ animationDelay: `${idx * 70}ms` }}
            >
              <div className="flex items-start justify-between gap-6">
                <div className="flex items-start gap-4">
                  <span className="mt-1 flex h-10 w-10 items-center justify-center rounded-full border border-gold/40 bg-background">
                    <Ticket className="h-4 w-4 text-gold" />
                  </span>
                  <div>
                    <h3 className="font-display text-xl text-foreground">
                      {ticketItem.ticketType.name}
                    </h3>
                    <p className="mt-2 font-mono text-[0.7rem] uppercase tracking-[0.2em] text-muted-foreground">
                      {ticketItem.id}
                    </p>
                  </div>
                </div>

                <div className="flex flex-col items-end gap-3">
                  <span className="inline-flex rounded-full border border-border bg-background px-3 py-1 text-[0.62rem] uppercase tracking-[0.24em] text-muted-foreground">
                    {ticketItem.status}
                  </span>
                  <div className="flex items-center gap-2 text-sm text-foreground">
                    <span className="font-display text-lg">
                      ${ticketItem.ticketType.price}
                    </span>
                    <ArrowUpRight className="h-4 w-4 text-gold transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                  </div>
                </div>
              </div>
            </Link>
          ))}

          {tickets && tickets.content.length === 0 && (
            <div className="rounded-lg border border-dashed border-border py-20 text-center">
              <p className="eyebrow">No tickets yet</p>
              <p className="mt-3 font-display text-2xl text-foreground">
                The collection awaits.
              </p>
              <Link to="/" className="mt-6 inline-block">
                <span className="link-underline text-[0.78rem] uppercase tracking-[0.24em] text-foreground">
                  Discover events
                </span>
              </Link>
            </div>
          )}
        </div>

        <div className="flex justify-center py-16">
          {tickets && (
            <SimplePagination pagination={tickets} onPageChange={setPage} />
          )}
        </div>
      </main>
    </div>
  );
};

export default DashboardListTickets;

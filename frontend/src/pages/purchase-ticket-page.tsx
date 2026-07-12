import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { getPublishedEvent, purchaseTicket } from "@/lib/api";
import {
  PublishedEventDetails,
  PublishedEventTicketTypeDetails,
} from "@/domain/domain";
import { format } from "date-fns";
import {
  AlertCircle,
  Calendar,
  CheckCircle2,
  CreditCard,
  MapPin,
  User,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { useNavigate, useParams } from "react-router";
import PublicNav from "@/components/public-nav";
import Ornament from "@/components/ornament";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";

const PurchaseTicketPage: React.FC = () => {
  const { eventId, ticketTypeId } = useParams();
  const { isLoading, user } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | undefined>();
  const [event, setEvent] = useState<PublishedEventDetails | undefined>();
  const [ticketType, setTicketType] = useState<
    PublishedEventTicketTypeDetails | undefined
  >();
  const [submitting, setSubmitting] = useState(false);
  const [isPurchaseSuccess, setIsPurchaseSuccess] = useState(false);

  useEffect(() => {
    if (!eventId) return;
    const load = async () => {
      try {
        const e = await getPublishedEvent(eventId);
        setEvent(e);
        const tt = e.ticketTypes.find((t) => t.id === ticketTypeId);
        setTicketType(tt);
        if (!tt) setError("That admission tier is no longer available.");
      } catch (err) {
        if (err instanceof Error) setError(err.message);
        else setError("We couldn't load the event details.");
      }
    };
    load();
  }, [eventId, ticketTypeId]);

  useEffect(() => {
    if (!isPurchaseSuccess) return;
    const timer = setTimeout(() => navigate("/dashboard/tickets"), 3000);
    return () => clearTimeout(timer);
  }, [isPurchaseSuccess, navigate]);

  const handlePurchase = async () => {
    if (isLoading || !user?.access_token || !eventId || !ticketTypeId) return;
    setError(undefined);
    setSubmitting(true);
    try {
      await purchaseTicket(user.access_token, eventId, ticketTypeId);
      setIsPurchaseSuccess(true);
    } catch (err) {
      if (err instanceof Error) setError(err.message);
      else setError("An unknown error occurred");
    } finally {
      setSubmitting(false);
    }
  };

  if (isPurchaseSuccess) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <PublicNav />
        <div className="container mx-auto flex items-center justify-center px-6 py-28">
          <div className="w-full max-w-md rounded-lg border border-border bg-card p-10 text-center reveal">
            <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full border border-gold/50">
              <CheckCircle2 className="h-7 w-7 text-gold" />
            </div>
            <p className="eyebrow mt-8">With our gratitude</p>
            <h2 className="mt-4 font-display text-3xl text-foreground">
              Your seat is held.
            </h2>
            <p className="mt-4 text-sm leading-relaxed text-muted-foreground">
              A confirmation has been recorded. We'll redirect you to your
              tickets in a moment.
            </p>
            <div className="my-6">
              <Ornament />
            </div>
            <Button
              onClick={() => navigate("/dashboard/tickets")}
              className="h-11 cursor-pointer rounded-full bg-ink px-6 text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90"
            >
              View my ticket
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <PublicNav />

      <main className="container mx-auto px-6 py-16">
        <div className="mx-auto grid max-w-5xl grid-cols-1 gap-12 md:grid-cols-12">
          {/* Order summary */}
          <aside className="md:col-span-5">
            <div className="sticky top-28 reveal rounded-lg border border-border bg-card p-8">
              <div className="flex items-center justify-between">
                <p className="eyebrow">Reservation</p>
                <Ornament variant="mark" />
              </div>
              <h2 className="mt-3 font-display text-3xl text-foreground">
                {event?.name ?? "Loading…"}
              </h2>
              {event && (
                <dl className="mt-6 space-y-3 text-sm text-muted-foreground">
                  <div className="flex items-start gap-3">
                    <MapPin className="mt-0.5 h-4 w-4 text-gold" />
                    <dd>{event.venue}</dd>
                  </div>
                  {event.start && event.end && (
                    <div className="flex items-start gap-3">
                      <Calendar className="mt-0.5 h-4 w-4 text-gold" />
                      <dd>
                        {format(event.start, "PPp")} —{" "}
                        {format(event.end, "p")}
                      </dd>
                    </div>
                  )}
                </dl>
              )}

              <div className="my-6">
                <Ornament />
              </div>

              <div className="space-y-3">
                <p className="eyebrow">Tier</p>
                <p className="font-display text-2xl text-foreground">
                  {ticketType?.name ?? "—"}
                </p>
                {ticketType?.description && (
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    {ticketType.description}
                  </p>
                )}
              </div>

              <div className="mt-8 flex items-baseline justify-between border-t border-border pt-6">
                <p className="eyebrow">Total</p>
                <p className="font-display text-3xl text-foreground">
                  ${ticketType?.price ?? "—"}
                </p>
              </div>
            </div>
          </aside>

          {/* Form */}
          <section className="md:col-span-7">
            <p className="eyebrow">Checkout</p>
            <h1 className="mt-3 font-display text-4xl text-foreground">
              Complete your reservation
            </h1>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
              A simulated checkout — no real card information is required nor
              stored.
            </p>

            <div className="mt-10 rounded-lg border border-border bg-card p-8">
              {error && (
                <Alert
                  variant="destructive"
                  className="mb-6 border-destructive/40 bg-destructive/5"
                >
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}

              <div className="space-y-6">
                <div className="space-y-2">
                  <Label className="eyebrow !text-[0.65rem]">
                    Card number
                  </Label>
                  <div className="relative">
                    <CreditCard className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      type="text"
                      placeholder="1234  5678  9012  3456"
                      maxLength={19}
                      className="h-11 rounded-md border-border bg-background pl-11"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label className="eyebrow !text-[0.65rem]">Cardholder</Label>
                  <div className="relative">
                    <User className="absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                    <Input
                      type="text"
                      placeholder="Full name"
                      className="h-11 rounded-md border-border bg-background pl-11"
                    />
                  </div>
                </div>

                <div className="my-2">
                  <Ornament />
                </div>

                <Button
                  onClick={handlePurchase}
                  disabled={submitting || !ticketType}
                  className="h-12 w-full cursor-pointer rounded-full bg-ink text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90 disabled:opacity-50"
                >
                  {submitting ? "Reserving…" : "Confirm reservation"}
                </Button>
                <p className="text-center text-[0.66rem] uppercase tracking-[0.28em] text-muted-foreground">
                  Demonstration only · No payment processed
                </p>
              </div>
            </div>
          </section>
        </div>
      </main>
    </div>
  );
};

export default PurchaseTicketPage;

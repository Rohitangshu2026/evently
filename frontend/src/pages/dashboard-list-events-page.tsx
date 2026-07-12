import NavBar from "@/components/nav-bar";
import { SimplePagination } from "@/components/simple-pagination";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Button } from "@/components/ui/button";
import {
  EventSummary,
  EventStatusEnum,
  SpringBootPagination,
} from "@/domain/domain";
import { deleteEvent, listEvents } from "@/lib/api";
import {
  AlertCircle,
  Calendar,
  Clock,
  Edit,
  MapPin,
  Plus,
  Tag,
  Trash,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth";
import { Link } from "react-router";
import { cn } from "@/lib/utils";
import Ornament from "@/components/ornament";

const DashboardListEventsPage: React.FC = () => {
  const { isLoading, user } = useAuth();
  const [events, setEvents] = useState<
    SpringBootPagination<EventSummary> | undefined
  >();
  const [error, setError] = useState<string | undefined>();
  const [deleteEventError, setDeleteEventError] = useState<
    string | undefined
  >();
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [eventToDelete, setEventToDelete] = useState<
    EventSummary | undefined
  >();

  useEffect(() => {
    if (isLoading || !user?.access_token) return;
    refreshEvents(user.access_token);
  }, [isLoading, user, page]);

  const refreshEvents = async (accessToken: string) => {
    try {
      setEvents(await listEvents(accessToken, page));
    } catch (err) {
      if (err instanceof Error) setError(err.message);
      else if (typeof err === "string") setError(err);
      else setError("An unknown error has occurred");
    }
  };

  const formatDate = (date?: Date) => {
    if (!date) return "TBD";
    return new Date(date).toLocaleDateString("en-US", {
      day: "numeric",
      month: "short",
      year: "numeric",
    });
  };

  const formatTime = (date?: Date) => {
    if (!date) return "";
    return new Date(date).toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const statusBadge = (status: EventStatusEnum) => {
    switch (status) {
      case EventStatusEnum.PUBLISHED:
        return "border-gold/50 bg-gold/10 text-foreground";
      case EventStatusEnum.CANCELLED:
        return "border-destructive/40 bg-destructive/10 text-destructive";
      case EventStatusEnum.COMPLETED:
        return "border-border bg-secondary text-muted-foreground";
      default:
        return "border-border bg-secondary text-muted-foreground";
    }
  };

  const handleOpenDeleteEventDialog = (eventToDelete: EventSummary) => {
    setEventToDelete(eventToDelete);
    setDialogOpen(true);
  };

  const handleCancelDeleteEventDialog = () => {
    setEventToDelete(undefined);
    setDialogOpen(false);
  };

  const handleDeleteEvent = async () => {
    if (!eventToDelete || isLoading || !user?.access_token) return;
    try {
      setDeleteEventError(undefined);
      await deleteEvent(user.access_token, eventToDelete.id);
      setEventToDelete(undefined);
      setDialogOpen(false);
      refreshEvents(user.access_token);
    } catch (err) {
      if (err instanceof Error) setDeleteEventError(err.message);
      else if (typeof err === "string") setDeleteEventError(err);
      else setDeleteEventError("An unknown error has occurred");
    }
  };

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

      <main className="container mx-auto max-w-4xl px-6 py-16">
        {/* Title */}
        <div className="reveal flex flex-col gap-6 border-b border-border pb-10 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="eyebrow">The studio</p>
            <div className="mt-3">
              <Ornament variant="mark" />
            </div>
            <h1 className="mt-3 font-display text-5xl text-foreground">
              Your events
            </h1>
            <p className="mt-3 max-w-md text-sm leading-relaxed text-muted-foreground">
              Drafts, published engagements, and past evenings — every gathering
              you've composed.
            </p>
          </div>
          <Link to="/dashboard/events/create">
            <Button className="h-11 cursor-pointer rounded-full bg-ink px-7 text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90">
              <Plus className="h-4 w-4" />
              New event
            </Button>
          </Link>
        </div>

        {/* Cards */}
        <div className="mt-10 space-y-5">
          {events?.content.map((eventItem, idx) => (
            <article
              key={eventItem.id}
              className="reveal group relative rounded-lg border border-border bg-card p-7 transition-all hover:border-gold/50 hover:-translate-y-0.5 hover:shadow-[0_22px_44px_-26px_rgba(60,40,10,0.4)]"
              style={{ animationDelay: `${idx * 70}ms` }}
            >
              <header className="flex items-start justify-between gap-6">
                <div>
                  <h3 className="font-display text-2xl text-foreground">
                    {eventItem.name}
                  </h3>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {eventItem.venue}
                  </p>
                </div>
                <span
                  className={cn(
                    "inline-flex items-center rounded-full border px-3 py-1 text-[0.62rem] uppercase tracking-[0.24em]",
                    statusBadge(eventItem.status),
                  )}
                >
                  {eventItem.status}
                </span>
              </header>

              <div className="gold-rule my-6" />

              <div className="grid grid-cols-1 gap-6 text-sm md:grid-cols-2">
                <div className="flex items-start gap-3">
                  <Calendar className="mt-0.5 h-4 w-4 text-gold" />
                  <div>
                    <p className="eyebrow">When</p>
                    <p className="mt-1 text-foreground">
                      {formatDate(eventItem.start)} → {formatDate(eventItem.end)}
                    </p>
                    <p className="text-xs text-muted-foreground">
                      {formatTime(eventItem.start)} — {formatTime(eventItem.end)}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <Clock className="mt-0.5 h-4 w-4 text-gold" />
                  <div>
                    <p className="eyebrow">Sales window</p>
                    <p className="mt-1 text-foreground">
                      {formatDate(eventItem.salesStart)} →{" "}
                      {formatDate(eventItem.salesEnd)}
                    </p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <MapPin className="mt-0.5 h-4 w-4 text-gold" />
                  <div>
                    <p className="eyebrow">Venue</p>
                    <p className="mt-1 text-foreground">{eventItem.venue}</p>
                  </div>
                </div>

                <div className="flex items-start gap-3">
                  <Tag className="mt-0.5 h-4 w-4 text-gold" />
                  <div>
                    <p className="eyebrow">Admissions</p>
                    <ul className="mt-1 space-y-0.5">
                      {eventItem.ticketTypes.map((tt) => (
                        <li
                          key={tt.id}
                          className="flex items-baseline gap-3 text-foreground"
                        >
                          <span>{tt.name}</span>
                          <span className="text-xs text-muted-foreground">
                            ${tt.price}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              </div>

              <footer className="mt-8 flex justify-end gap-2">
                <Link to={`/dashboard/events/update/${eventItem.id}`}>
                  <Button
                    type="button"
                    variant="ghost"
                    className="h-9 cursor-pointer rounded-full border border-border bg-background px-4 text-[0.7rem] uppercase tracking-[0.22em] hover:border-gold hover:text-gold"
                  >
                    <Edit className="h-3.5 w-3.5" /> Edit
                  </Button>
                </Link>
                <Button
                  type="button"
                  variant="ghost"
                  className="h-9 cursor-pointer rounded-full border border-destructive/30 bg-background px-4 text-[0.7rem] uppercase tracking-[0.22em] text-destructive hover:bg-destructive/10"
                  onClick={() => handleOpenDeleteEventDialog(eventItem)}
                >
                  <Trash className="h-3.5 w-3.5" /> Remove
                </Button>
              </footer>
            </article>
          ))}

          {events && events.content.length === 0 && (
            <div className="rounded-lg border border-dashed border-border py-20 text-center">
              <Ornament variant="mark" />
              <p className="eyebrow mt-6">A quiet stage</p>
              <p className="mt-3 font-display text-2xl text-foreground">
                No events yet. Begin with one.
              </p>
            </div>
          )}
        </div>

        <div className="flex justify-center py-16">
          {events && (
            <SimplePagination pagination={events} onPageChange={setPage} />
          )}
        </div>
      </main>

      <AlertDialog open={dialogOpen}>
        <AlertDialogContent className="border-border bg-card">
          <AlertDialogHeader>
            <AlertDialogTitle className="font-display text-2xl">
              Remove this event?
            </AlertDialogTitle>
            <AlertDialogDescription className="text-muted-foreground">
              This will permanently delete '{eventToDelete?.name}'. The action
              cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteEventError && (
            <Alert variant="destructive" className="border-destructive/40">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{deleteEventError}</AlertDescription>
            </Alert>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel
              onClick={handleCancelDeleteEventDialog}
              className="rounded-full"
            >
              Keep
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => handleDeleteEvent()}
              className="rounded-full bg-destructive text-white hover:bg-destructive/90"
            >
              Remove
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
};

export default DashboardListEventsPage;

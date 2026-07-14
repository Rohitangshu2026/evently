import { Button } from "../components/ui/button";
import { Input } from "@/components/ui/input";
import { AlertCircle, Search } from "lucide-react";
import { useEffect, useState } from "react";
import { PublishedEventSummary, SpringBootPagination } from "@/domain/domain";
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import PublishedEventCard from "@/components/published-event-card";
import { SimplePagination } from "@/components/simple-pagination";
import PublicNav from "@/components/public-nav";
import Ornament from "@/components/ornament";

const AttendeeLandingPage: React.FC = () => {
  const [page, setPage] = useState(0);
  const [publishedEvents, setPublishedEvents] = useState<
    SpringBootPagination<PublishedEventSummary> | undefined
  >();
  const [error, setError] = useState<string | undefined>();
  // What's currently in the search box.
  const [searchTerm, setSearchTerm] = useState("");
  // The query actually committed (on submit) — this, plus page, drives the fetch.
  const [submittedQuery, setSubmittedQuery] = useState("");

  const handleErr = (err: unknown) => {
    if (err instanceof Error) setError(err.message);
    else if (typeof err === "string") setError(err);
    else setError("An unknown error has occurred");
  };

  // A single effect owns all fetching. The `ignore` guard drops responses from
  // a superseded request, so a slow browse can't overwrite a newer search
  // (the race that made search look broken on the deployed site).
  useEffect(() => {
    let ignore = false;
    const load = async () => {
      try {
        setError(undefined);
        const trimmed = submittedQuery.trim();
        const data =
          trimmed.length > 0
            ? await searchPublishedEvents(trimmed, page)
            : await listPublishedEvents(page);
        if (!ignore) setPublishedEvents(data);
      } catch (err) {
        if (!ignore) handleErr(err);
      }
    };
    load();
    return () => {
      ignore = true;
    };
  }, [submittedQuery, page]);

  const handleSearchSubmit = () => {
    // Reset to the first page so a search never lands on an out-of-range page.
    setPage(0);
    setSubmittedQuery(searchTerm);
  };

  if (error) {
    return (
      <div className="min-h-screen bg-background text-foreground">
        <PublicNav />
        <div className="container mx-auto px-6 py-16">
          <Alert variant="destructive" className="border-destructive/40 bg-card">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>An error occurred</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background text-foreground">
      <PublicNav />

      {/* Hero */}
      <section className="container mx-auto px-6 pt-10 pb-8 md:pt-14 md:pb-10">
        <div className="mx-auto max-w-4xl text-center">
          <p className="eyebrow reveal">An invitation</p>
          <div className="reveal reveal-delay-1 mt-3 flex justify-center">
            <Ornament variant="mark" className="opacity-80" />
          </div>
          <h1 className="reveal reveal-delay-1 mt-3 font-display text-4xl leading-[1.05] text-foreground md:text-6xl">
            Events worth
            <br />
            <span className="italic text-gold">remembering</span>.
          </h1>
          <p className="reveal reveal-delay-2 mx-auto mt-6 max-w-xl text-base leading-relaxed text-muted-foreground md:text-lg">
            Discover concerts, dinners, and gatherings near you — browse events,
            book tickets, and carry a QR pass for the door.
          </p>

          <form
            onSubmit={(e) => {
              e.preventDefault();
              handleSearchSubmit();
            }}
            className="reveal reveal-delay-3 mx-auto mt-12 flex max-w-xl items-center gap-2 rounded-full border border-border bg-card/85 p-1.5 shadow-[0_18px_40px_-24px_rgba(60,40,10,0.45)] backdrop-blur"
          >
            <Search className="ml-4 h-4 w-4 text-muted-foreground" />
            <Input
              className="h-10 flex-1 border-0 bg-transparent text-sm shadow-none focus-visible:ring-0"
              placeholder="Search by event or venue…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            {submittedQuery && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setSearchTerm("");
                  setPage(0);
                  setSubmittedQuery("");
                }}
                className="h-10 cursor-pointer rounded-full px-4 text-[0.72rem] uppercase tracking-[0.2em] text-muted-foreground hover:text-foreground"
              >
                Clear
              </Button>
            )}
            <Button
              type="submit"
              className="h-10 cursor-pointer rounded-full bg-ink px-6 text-[0.74rem] uppercase tracking-[0.22em] text-primary-foreground hover:bg-ink/90"
            >
              Search
            </Button>
          </form>

          <div className="reveal reveal-delay-4 mt-10 flex items-center justify-center gap-6 text-[0.7rem] uppercase tracking-[0.28em] text-muted-foreground">
            <span>Curated weekly</span>
            <span className="h-px w-6 bg-border" />
            <span>Trusted by hosts</span>
            <span className="h-px w-6 bg-border" />
            <span>QR-secured entry</span>
          </div>
        </div>
      </section>

      {/* Editorial intro with drop cap */}
      <section className="container mx-auto px-6 py-20">
        <div className="mx-auto max-w-3xl">
          <p className="dropcap font-display text-[1.15rem] leading-[1.9] text-foreground md:text-[1.22rem]">
            The best evenings are not announced — they are arranged. A small
            room, a careful guest list, a host who has thought of everything
            and shows none of it. We keep our collection small, on purpose.
            Each event below has been chosen for the same reason: it is worth
            the journey, the dress, and the late hour home.
          </p>
          <div className="mt-8 flex items-center gap-4">
            <span className="h-px w-12 bg-gold/60" />
            <span className="font-display text-sm italic text-muted-foreground">
              — Evently
            </span>
          </div>
        </div>
      </section>

      <div className="container mx-auto px-6">
        <Ornament />
      </div>

      {/* Section header */}
      <section className="container mx-auto px-6 pt-16">
        <div className="flex items-end justify-between border-b border-border pb-6">
          <div>
            <p className="eyebrow">The collection</p>
            <h2 className="mt-2 font-display text-3xl text-foreground md:text-4xl">
              Currently on offer
            </h2>
          </div>
          <p className="hidden max-w-xs text-right text-sm leading-relaxed text-muted-foreground md:block">
            Each event hand-selected for atmosphere, craft, and the company you
            keep.
          </p>
        </div>
      </section>

      {/* Cards */}
      <section className="container mx-auto px-6 py-12">
        <div className="grid grid-cols-1 gap-8 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
          {publishedEvents?.content?.map((publishedEvent, idx) => (
            <div
              key={publishedEvent.id}
              className="reveal"
              style={{ animationDelay: `${Math.min(idx * 80, 600)}ms` }}
            >
              <PublishedEventCard
                publishedEvent={publishedEvent}
                index={idx}
              />
            </div>
          ))}
        </div>

        {publishedEvents && publishedEvents.content?.length === 0 && (
          <div className="rounded-lg border border-dashed border-border py-20 text-center">
            <Ornament variant="mark" />
            <p className="eyebrow mt-6">Empty for now</p>
            <p className="mt-3 font-display text-2xl text-foreground">
              No events match your search.
            </p>
          </div>
        )}

        {publishedEvents && (
          <div className="flex w-full justify-center py-16">
            <SimplePagination
              pagination={publishedEvents}
              onPageChange={setPage}
            />
          </div>
        )}
      </section>

      {/* Footer */}
      <footer className="border-t border-border bg-cream/40">
        <div className="container mx-auto px-6 py-12">
          <Ornament className="opacity-70" />
          <div className="mt-8 flex flex-col items-center justify-between gap-4 text-[0.72rem] uppercase tracking-[0.26em] text-muted-foreground md:flex-row">
            <span>Evently · Tickets to remember</span>
            <span>© {new Date().getFullYear()}</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default AttendeeLandingPage;

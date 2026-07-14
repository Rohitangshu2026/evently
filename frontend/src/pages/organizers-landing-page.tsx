import { Button } from "@/components/ui/button";
import PublicNav from "@/components/public-nav";
import Ornament from "@/components/ornament";
import { CalendarPlus, QrCode, Ticket } from "lucide-react";
import { useNavigate } from "react-router";

const features = [
  {
    icon: CalendarPlus,
    title: "Effortless creation",
    body: "Create events with schedules, venues, and multiple pricing tiers in a clean, focused editor.",
  },
  {
    icon: Ticket,
    title: "Refined ticketing",
    body: "Set capacity per tier, open and close sales windows, and track what's sold — no overselling, ever.",
  },
  {
    icon: QrCode,
    title: "Doorside discretion",
    body: "Scan QR passes at entry for instant, tamper-proof validation. Each ticket admits exactly once.",
  },
];

const OrganizersLandingPage: React.FC = () => {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background text-foreground">
      <PublicNav dashboardPath="/dashboard/events" />

      <main className="container mx-auto px-6">
        {/* Hero */}
        <section className="grid grid-cols-1 items-center gap-12 py-12 md:grid-cols-2 md:py-16">
          <div className="space-y-6">
            <p className="eyebrow reveal">For organizers & hosts</p>
            <div className="reveal reveal-delay-1">
              <Ornament variant="mark" />
            </div>
            <h1 className="reveal reveal-delay-1 font-display text-4xl leading-[1.05] text-foreground md:text-5xl">
              Create events people
              <br />
              <span className="italic text-gold">remember</span>.
            </h1>
            <p className="reveal reveal-delay-2 max-w-md text-base leading-relaxed text-muted-foreground md:text-lg">
              Build event pages, set ticket tiers and capacity, sell admissions
              online, and validate guests at the door with QR passes — all in
              one place.
            </p>
            <div className="reveal reveal-delay-3 flex flex-wrap items-center gap-4 pt-2">
              <Button
                className="h-11 cursor-pointer rounded-full bg-ink px-7 text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90"
                onClick={() => navigate("/dashboard/events")}
              >
                Create an event
              </Button>
              <Button
                variant="ghost"
                className="h-11 cursor-pointer rounded-full border border-border bg-card px-7 text-[0.74rem] uppercase tracking-[0.24em] text-foreground hover:bg-cream"
                onClick={() => navigate("/")}
              >
                Browse the collection
              </Button>
            </div>
          </div>

          <div className="reveal reveal-delay-2 relative">
            <div className="absolute -left-6 -top-6 hidden h-32 w-32 rounded-full border border-gold/40 md:block" />
            <div className="absolute -bottom-6 -right-6 hidden h-40 w-40 rounded-full border border-gold/30 md:block" />
            <div
              className="relative overflow-hidden rounded-lg border border-border bg-card shadow-[0_40px_80px_-40px_rgba(60,40,10,0.5)]"
              style={{ animation: "soft-float 7s ease-in-out infinite" }}
            >
              <div className="relative aspect-[4/5] w-full">
                <img
                  src="organizers-landing-hero.png"
                  alt="A gathering in soft light"
                  className="h-full w-full object-cover"
                />
                {/* Corner brackets */}
                <span className="pointer-events-none absolute left-3 top-3 h-5 w-5 border-l border-t border-gold/70" />
                <span className="pointer-events-none absolute right-3 top-3 h-5 w-5 border-r border-t border-gold/70" />
                <span className="pointer-events-none absolute bottom-3 left-3 h-5 w-5 border-b border-l border-gold/70" />
                <span className="pointer-events-none absolute bottom-3 right-3 h-5 w-5 border-b border-r border-gold/70" />
              </div>
              <div className="flex items-center justify-between border-t border-border bg-background/85 px-6 py-4 backdrop-blur">
                <span className="eyebrow">Plate 01</span>
                <span className="font-display text-sm italic text-muted-foreground">
                  An evening, well-kept.
                </span>
              </div>
            </div>
          </div>
        </section>
      </main>

      <main className="container mx-auto px-6">
        <div className="py-12">
          <Ornament />
        </div>

        {/* Features */}
        <section className="py-12">
          <div className="mb-16 max-w-2xl">
            <p className="eyebrow">The studio</p>
            <h2 className="mt-3 font-display text-4xl text-foreground md:text-5xl">
              Tools, with restraint.
            </h2>
          </div>
          <div className="grid grid-cols-1 gap-12 md:grid-cols-3">
            {features.map((f, i) => {
              const Icon = f.icon;
              return (
                <article
                  key={f.title}
                  className="reveal group relative space-y-4 border-t border-border pt-8"
                  style={{ animationDelay: `${i * 120}ms` }}
                >
                  <Icon
                    className="h-6 w-6 text-gold"
                    strokeWidth={1.5}
                    aria-hidden
                  />
                  <span className="block font-display text-sm italic tracking-[0.2em] text-gold">
                    Chapter {String(i + 1).padStart(2, "0")}
                  </span>
                  <h3 className="font-display text-2xl text-foreground transition-colors group-hover:text-gold">
                    {f.title}
                  </h3>
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    {f.body}
                  </p>
                </article>
              );
            })}
          </div>
        </section>

        <div className="py-12">
          <Ornament />
        </div>

        {/* CTA */}
        <section className="py-20 text-center">
          <p className="eyebrow">Ready when you are</p>
          <h2 className="mx-auto mt-6 max-w-3xl font-display text-4xl text-foreground md:text-5xl">
            Host your next event.{" "}
            <span className="italic text-gold">Create it in minutes.</span>
          </h2>
          <Button
            className="mt-10 h-12 cursor-pointer rounded-full bg-ink px-8 text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90"
            onClick={() => navigate("/dashboard/events")}
          >
            Create an event
          </Button>
        </section>
      </main>

      <footer className="border-t border-border bg-cream/40">
        <div className="container mx-auto px-6 py-12">
          <Ornament className="opacity-70" />
          <div className="mt-8 flex flex-col items-center justify-between gap-4 text-[0.72rem] uppercase tracking-[0.26em] text-muted-foreground md:flex-row">
            <span>Evently · For hosts</span>
            <span className="font-display italic normal-case tracking-[0.18em]">
              The room remembers what you've planned.
            </span>
            <span>© {new Date().getFullYear()}</span>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default OrganizersLandingPage;

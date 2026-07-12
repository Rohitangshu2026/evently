/**
 * Combined sign-in / sign-up page backed by the Evently API's own auth
 * endpoints (no external identity provider). On success the visitor is
 * returned to wherever ProtectedRoute intercepted them, or the dashboard.
 */
import { FormEvent, useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { useAuth } from "@/lib/auth";
import Wordmark from "@/components/wordmark";
import Ornament from "@/components/ornament";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

type Mode = "sign-in" | "sign-up";

const ROLE_OPTIONS = [
  { value: "ATTENDEE", label: "Attendee — discover events & buy tickets" },
  { value: "ORGANIZER", label: "Organizer — create & manage events" },
  { value: "STAFF", label: "Staff — validate tickets at the door" },
];

const LoginPage: React.FC = () => {
  const { isLoading, isAuthenticated, signIn, signUp } = useAuth();
  const navigate = useNavigate();

  const [mode, setMode] = useState<Mode>("sign-in");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [name, setName] = useState("");
  const [role, setRole] = useState("ATTENDEE");
  const [error, setError] = useState<string | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Already signed in (or just signed in): leave the login page.
  useEffect(() => {
    if (isLoading || !isAuthenticated) return;
    const redirectPath = localStorage.getItem("redirectPath");
    localStorage.removeItem("redirectPath");
    navigate(redirectPath ?? "/dashboard", { replace: true });
  }, [isLoading, isAuthenticated, navigate]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setError(undefined);
    setIsSubmitting(true);
    try {
      if (mode === "sign-in") {
        await signIn(email, password);
      } else {
        await signUp(email, password, name, role);
      }
      // Navigation happens in the effect above once isAuthenticated flips.
    } catch (err) {
      setError(err instanceof Error ? err.message : "Something went wrong.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-6 py-12 text-foreground">
      <div className="flex w-full max-w-md flex-col items-center gap-8 reveal">
        <Wordmark to="/" tagline />
        <div className="w-64">
          <Ornament />
        </div>

        <div className="w-full space-y-6">
          <div className="text-center">
            <p className="eyebrow">
              {mode === "sign-in" ? "Welcome back" : "Join us"}
            </p>
            <h1 className="mt-2 font-display text-3xl italic">
              {mode === "sign-in" ? "Sign in" : "Create your account"}
            </h1>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {mode === "sign-up" && (
              <div className="space-y-2">
                <Label htmlFor="name">Name</Label>
                <Input
                  id="name"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="Your name"
                  required
                  autoComplete="name"
                />
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                required
                autoComplete="email"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="password">Password</Label>
              <Input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={
                  mode === "sign-up" ? "At least 8 characters" : "Your password"
                }
                required
                minLength={mode === "sign-up" ? 8 : undefined}
                autoComplete={
                  mode === "sign-in" ? "current-password" : "new-password"
                }
              />
            </div>

            {mode === "sign-up" && (
              <div className="space-y-2">
                <Label htmlFor="role">I am an…</Label>
                <Select value={role} onValueChange={setRole}>
                  <SelectTrigger id="role" className="w-full">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLE_OPTIONS.map((option) => (
                      <SelectItem key={option.value} value={option.value}>
                        {option.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {error && (
              <p className="text-sm text-destructive" role="alert">
                {error}
              </p>
            )}

            <Button type="submit" className="w-full" disabled={isSubmitting}>
              {isSubmitting
                ? "One moment…"
                : mode === "sign-in"
                  ? "Sign in"
                  : "Create account"}
            </Button>
          </form>

          <p className="text-center text-sm text-muted-foreground">
            {mode === "sign-in" ? (
              <>
                New to Evently?{" "}
                <button
                  type="button"
                  className="text-gold underline-offset-4 hover:underline"
                  onClick={() => {
                    setMode("sign-up");
                    setError(undefined);
                  }}
                >
                  Create an account
                </button>
              </>
            ) : (
              <>
                Already have an account?{" "}
                <button
                  type="button"
                  className="text-gold underline-offset-4 hover:underline"
                  onClick={() => {
                    setMode("sign-in");
                    setError(undefined);
                  }}
                >
                  Sign in
                </button>
              </>
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;

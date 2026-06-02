import NavBar from "@/components/nav-bar";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useState } from "react";
import { Scanner } from "@yudiel/react-qr-scanner";
import {
  TicketValidationMethod,
  TicketValidationStatus,
} from "@/domain/domain";
import { AlertCircle, Check, RotateCw, X } from "lucide-react";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { validateTicket } from "@/lib/api";
import { useAuth } from "react-oidc-context";

const DashboardValidateQrPage: React.FC = () => {
  const { user } = useAuth();
  const [isManual, setIsManual] = useState(false);
  const [data, setData] = useState<string | undefined>();
  const [error, setError] = useState<string | undefined>();
  const [validationStatus, setValidationStatus] = useState<
    TicketValidationStatus | undefined
  >();

  const handleReset = () => {
    setIsManual(false);
    setData(undefined);
    setError(undefined);
    setValidationStatus(undefined);
  };

  const handleError = (err: unknown) => {
    if (err instanceof Error) setError(err.message);
    else if (typeof err === "string") setError(err);
    else setError("An unknown error occurred");
  };

  const handleValidate = async (id: string, method: TicketValidationMethod) => {
    if (!user?.access_token) return;
    try {
      const response = await validateTicket(user.access_token, { id, method });
      setValidationStatus(response.status);
    } catch (err) {
      handleError(err);
    }
  };

  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />

      <main className="container mx-auto px-6 py-16">
        <div className="mx-auto max-w-md">
          <div className="text-center">
            <p className="eyebrow">At the door</p>
            <h1 className="mt-3 font-display text-4xl text-foreground">
              Welcome guests in.
            </h1>
            <p className="mt-3 text-sm text-muted-foreground">
              Scan the QR or enter a reference manually.
            </p>
          </div>

          {error && (
            <Alert
              variant="destructive"
              className="mt-8 border-destructive/40 bg-card"
            >
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Scanner viewport */}
          <div className="relative mt-8 overflow-hidden rounded-lg border border-border bg-card">
            <div className="aspect-square">
              <Scanner
                key={`scanner-${data}-${validationStatus}`}
                onScan={(result) => {
                  if (result) {
                    const qrCodeId = result[0].rawValue;
                    setData(qrCodeId);
                    handleValidate(qrCodeId, TicketValidationMethod.QR_SCAN);
                  }
                }}
                onError={handleError}
                styles={{
                  container: { width: "100%", height: "100%" },
                  video: { objectFit: "cover" },
                }}
              />
            </div>

            {/* Corner brackets */}
            <span className="pointer-events-none absolute left-4 top-4 h-6 w-6 border-l border-t border-gold" />
            <span className="pointer-events-none absolute right-4 top-4 h-6 w-6 border-r border-t border-gold" />
            <span className="pointer-events-none absolute bottom-4 left-4 h-6 w-6 border-b border-l border-gold" />
            <span className="pointer-events-none absolute bottom-4 right-4 h-6 w-6 border-b border-r border-gold" />

            {validationStatus && (
              <div className="absolute inset-0 flex items-center justify-center bg-background/85 backdrop-blur-sm">
                {validationStatus === TicketValidationStatus.VALID ? (
                  <div className="text-center">
                    <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full border border-gold/60 bg-gold/15">
                      <Check className="h-10 w-10 text-gold" />
                    </div>
                    <p className="eyebrow mt-6">Admitted</p>
                    <p className="mt-2 font-display text-2xl text-foreground">
                      Welcome, please come in.
                    </p>
                  </div>
                ) : (
                  <div className="text-center">
                    <div className="mx-auto flex h-20 w-20 items-center justify-center rounded-full border border-destructive/50 bg-destructive/15">
                      <X className="h-10 w-10 text-destructive" />
                    </div>
                    <p className="eyebrow mt-6 text-destructive">Declined</p>
                    <p className="mt-2 font-display text-2xl text-foreground">
                      This ticket is not valid.
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Status / Manual */}
          <div className="mt-6">
            {isManual ? (
              <div className="space-y-4">
                <Input
                  className="h-12 rounded-md border-border bg-card font-mono text-sm"
                  placeholder="Enter ticket reference"
                  value={data ?? ""}
                  onChange={(e) => setData(e.target.value)}
                />
                <Button
                  className="h-12 w-full cursor-pointer rounded-full bg-ink text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90"
                  onClick={() =>
                    handleValidate(data || "", TicketValidationMethod.MANUAL)
                  }
                >
                  Submit reference
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                <div className="flex h-12 items-center justify-center rounded-md border border-dashed border-border bg-card px-4 font-mono text-xs tracking-wider text-muted-foreground">
                  {data || "AWAITING SCAN…"}
                </div>
                <Button
                  variant="ghost"
                  className="h-11 w-full cursor-pointer rounded-full border border-border bg-card text-[0.74rem] uppercase tracking-[0.24em] text-foreground hover:border-gold hover:text-gold"
                  onClick={() => setIsManual(true)}
                >
                  Enter manually
                </Button>
              </div>
            )}

            <Button
              variant="ghost"
              className="mt-3 h-11 w-full cursor-pointer rounded-full text-[0.74rem] uppercase tracking-[0.24em] text-muted-foreground hover:text-foreground"
              onClick={handleReset}
            >
              <RotateCw className="h-3.5 w-3.5" />
              Reset
            </Button>
          </div>
        </div>
      </main>
    </div>
  );
};

export default DashboardValidateQrPage;

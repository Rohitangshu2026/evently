import NavBar from "@/components/nav-bar";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import {
  CreateEventRequest,
  CreateTicketTypeRequest,
  EventDetails,
  EventStatusEnum,
  UpdateEventRequest,
  UpdateTicketTypeRequest,
} from "@/domain/domain";
import { createEvent, getEvent, updateEvent } from "@/lib/api";
import { format } from "date-fns";
import {
  AlertCircle,
  CalendarIcon,
  Edit,
  Plus,
  Ticket,
  Trash,
} from "lucide-react";
import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { useNavigate, useParams } from "react-router";

interface DateTimeSelectProperties {
  date: Date | undefined;
  setDate: (date: Date) => void;
  time: string | undefined;
  setTime: (time: string) => void;
  enabled: boolean;
  setEnabled: (isEnabled: boolean) => void;
}

const DateTimeSelect: React.FC<DateTimeSelectProperties> = ({
  date,
  setDate,
  time,
  setTime,
  enabled,
  setEnabled,
}) => {
  return (
    <div className="flex items-center gap-3">
      <Switch checked={enabled} onCheckedChange={setEnabled} />

      {enabled && (
        <div className="flex w-full gap-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button
                variant="ghost"
                className="h-10 cursor-pointer rounded-md border border-border bg-background px-3 text-sm font-normal text-foreground hover:bg-cream"
              >
                <CalendarIcon className="h-4 w-4 text-gold" />
                {date ? format(date, "PPP") : <span>Pick a date</span>}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0" align="start">
              <Calendar
                mode="single"
                selected={date}
                onSelect={(selectedDate) => {
                  if (!selectedDate) return;
                  const y = selectedDate.getFullYear();
                  const m = selectedDate.getMonth();
                  const d = selectedDate.getDate();
                  setDate(new Date(Date.UTC(y, m, d)));
                }}
                className="rounded-md border"
              />
            </PopoverContent>
          </Popover>

          <Input
            type="time"
            className="h-10 w-[110px] rounded-md border-border bg-background text-sm"
            value={time}
            onChange={(e) => setTime(e.target.value)}
          />
        </div>
      )}
    </div>
  );
};

const generateTempId = () => `temp_${crypto.randomUUID()}`;
const isTempId = (id: string | undefined) => id && id.startsWith("temp_");

interface TicketTypeData {
  id: string | undefined;
  name: string;
  price: number;
  totalAvailable?: number;
  description: string;
}

interface EventData {
  id: string | undefined;
  name: string;
  startDate: Date | undefined;
  startTime: string | undefined;
  endDate: Date | undefined;
  endTime: string | undefined;
  venueDetails: string;
  salesStartDate: Date | undefined;
  salesStartTime: string | undefined;
  salesEndDate: Date | undefined;
  salesEndTime: string | undefined;
  ticketTypes: TicketTypeData[];
  status: EventStatusEnum;
  createdAt: Date | undefined;
  updatedAt: Date | undefined;
}

const formatTimeFromDate = (date: Date): string => {
  const hours = date.getHours().toString().padStart(2, "0");
  const minutes = date.getMinutes().toString().padStart(2, "0");
  return `${hours}:${minutes}`;
};

const combineDateTime = (date: Date, time: string): Date => {
  const [hours, minutes] = time
    .split(":")
    .map((num) => Number.parseInt(num, 10));

  return new Date(
    Date.UTC(
      date.getFullYear(),
      date.getMonth(),
      date.getDate(),
      hours,
      minutes,
      0,
      0,
    ),
  );
};

interface SectionProps {
  index: string;
  title: string;
  description: string;
  children: React.ReactNode;
}

const Section: React.FC<SectionProps> = ({ index, title, description, children }) => (
  <section className="grid grid-cols-1 gap-8 border-t border-border py-10 md:grid-cols-12">
    <div className="md:col-span-4">
      <p className="font-display text-xs tracking-[0.24em] text-gold">{index}</p>
      <h2 className="mt-3 font-display text-2xl text-foreground">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
        {description}
      </p>
    </div>
    <div className="space-y-5 md:col-span-8">{children}</div>
  </section>
);

const DashboardManageEventPage: React.FC = () => {
  const { isLoading, user } = useAuth();
  const { id } = useParams();
  const isEditMode = !!id;
  const navigate = useNavigate();

  const [eventData, setEventData] = useState<EventData>({
    id: undefined,
    name: "",
    startDate: undefined,
    startTime: undefined,
    endDate: undefined,
    endTime: undefined,
    venueDetails: "",
    salesStartDate: undefined,
    salesStartTime: undefined,
    salesEndDate: undefined,
    salesEndTime: undefined,
    ticketTypes: [],
    status: EventStatusEnum.DRAFT,
    createdAt: undefined,
    updatedAt: undefined,
  });

  const [currentTicketType, setCurrentTicketType] = useState<
    TicketTypeData | undefined
  >();
  const [dialogOpen, setDialogOpen] = useState(false);
  const [eventDateEnabled, setEventDateEnabled] = useState(false);
  const [eventSalesDateEnabled, setEventSalesDateEnabled] = useState(false);
  const [error, setError] = useState<string | undefined>();

  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const updateField = (field: keyof EventData, value: any) => {
    setEventData((prev) => ({ ...prev, [field]: value }));
  };

  useEffect(() => {
    if (isEditMode && !isLoading && user?.access_token) {
      const fetchEvent = async () => {
        const event: EventDetails = await getEvent(user.access_token, id);
        setEventData({
          id: event.id,
          name: event.name,
          startDate: event.start,
          startTime: event.start
            ? formatTimeFromDate(new Date(event.start))
            : undefined,
          endDate: event.end,
          endTime: event.end
            ? formatTimeFromDate(new Date(event.end))
            : undefined,
          venueDetails: event.venue,
          salesStartDate: event.salesStart,
          salesStartTime: event.salesStart
            ? formatTimeFromDate(new Date(event.salesStart))
            : undefined,
          salesEndDate: event.salesEnd,
          salesEndTime: event.salesEnd
            ? formatTimeFromDate(new Date(event.salesEnd))
            : undefined,
          status: event.status,
          ticketTypes: event.ticketTypes.map((ticket) => ({
            id: ticket.id,
            name: ticket.name,
            description: ticket.description,
            price: ticket.price,
            totalAvailable: ticket.totalAvailable,
          })),
          createdAt: event.createdAt,
          updatedAt: event.updatedAt,
        });
        setEventDateEnabled(!!(event.start || event.end));
        setEventSalesDateEnabled(!!(event.salesStart || event.salesEnd));
      };
      fetchEvent();
    }
  }, [id, user]);

  const handleEventUpdateSubmit = async (accessToken: string, id: string) => {
    const ticketTypes: UpdateTicketTypeRequest[] = eventData.ticketTypes.map(
      (tt) => ({
        id: isTempId(tt.id) ? undefined : tt.id,
        name: tt.name,
        price: tt.price,
        description: tt.description,
        totalAvailable: tt.totalAvailable,
      }),
    );

    const request: UpdateEventRequest = {
      id,
      name: eventData.name,
      start:
        eventData.startDate && eventData.startTime
          ? combineDateTime(eventData.startDate, eventData.startTime)
          : undefined,
      end:
        eventData.endDate && eventData.endTime
          ? combineDateTime(eventData.endDate, eventData.endTime)
          : undefined,
      venue: eventData.venueDetails,
      salesStart:
        eventData.salesStartDate && eventData.salesStartTime
          ? combineDateTime(eventData.salesStartDate, eventData.salesStartTime)
          : undefined,
      salesEnd:
        eventData.salesEndDate && eventData.salesEndTime
          ? combineDateTime(eventData.salesEndDate, eventData.salesEndTime)
          : undefined,
      status: eventData.status,
      ticketTypes,
    };

    try {
      await updateEvent(accessToken, id, request);
      navigate("/dashboard/events");
    } catch (err) {
      if (err instanceof Error) setError(err.message);
      else if (typeof err === "string") setError(err);
      else setError("An unknown error occurred");
    }
  };

  const handleEventCreateSubmit = async (accessToken: string) => {
    const ticketTypes: CreateTicketTypeRequest[] = eventData.ticketTypes.map(
      (tt) => ({
        name: tt.name,
        price: tt.price,
        description: tt.description,
        totalAvailable: tt.totalAvailable,
      }),
    );

    const request: CreateEventRequest = {
      name: eventData.name,
      start:
        eventData.startDate && eventData.startTime
          ? combineDateTime(eventData.startDate, eventData.startTime)
          : undefined,
      end:
        eventData.endDate && eventData.endTime
          ? combineDateTime(eventData.endDate, eventData.endTime)
          : undefined,
      venue: eventData.venueDetails,
      salesStart:
        eventData.salesStartDate && eventData.salesStartTime
          ? combineDateTime(eventData.salesStartDate, eventData.salesStartTime)
          : undefined,
      salesEnd:
        eventData.salesEndDate && eventData.salesEndTime
          ? combineDateTime(eventData.salesEndDate, eventData.salesEndTime)
          : undefined,
      status: eventData.status,
      ticketTypes,
    };

    try {
      await createEvent(accessToken, request);
      navigate("/dashboard/events");
    } catch (err) {
      if (err instanceof Error) setError(err.message);
      else if (typeof err === "string") setError(err);
      else setError("An unknown error occurred");
    }
  };

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(undefined);

    if (isLoading || !user || !user.access_token) {
      console.error("User not found!");
      return;
    }

    if (isEditMode) {
      if (!eventData.id) {
        setError("Event does not have an ID");
        return;
      }
      await handleEventUpdateSubmit(user.access_token, eventData.id);
    } else {
      await handleEventCreateSubmit(user.access_token);
    }
  };

  const handleAddTicketType = () => {
    setCurrentTicketType({
      id: undefined,
      name: "",
      price: 0,
      totalAvailable: 0,
      description: "",
    });
    setDialogOpen(true);
  };

  const handleSaveTicketType = () => {
    if (!currentTicketType) return;
    const newTicketTypes = [...eventData.ticketTypes];

    if (currentTicketType.id) {
      const index = newTicketTypes.findIndex(
        (t) => t.id === currentTicketType.id,
      );
      if (index !== -1) newTicketTypes[index] = currentTicketType;
    } else {
      newTicketTypes.push({ ...currentTicketType, id: generateTempId() });
    }

    updateField("ticketTypes", newTicketTypes);
    setDialogOpen(false);
  };

  const handleEditTicketType = (ticketType: TicketTypeData) => {
    setCurrentTicketType(ticketType);
    setDialogOpen(true);
  };

  const handleDeleteTicketType = (id: string | undefined) => {
    if (!id) return;
    updateField(
      "ticketTypes",
      eventData.ticketTypes.filter((t) => t.id !== id),
    );
  };

  return (
    <div className="min-h-screen bg-background text-foreground">
      <NavBar />

      <main className="container mx-auto max-w-5xl px-6 py-16">
        <div className="flex flex-col gap-3 border-b border-border pb-10">
          <p className="eyebrow">{isEditMode ? "Editing" : "New composition"}</p>
          <h1 className="font-display text-5xl text-foreground">
            {isEditMode ? "Refine the evening" : "Compose an event"}
          </h1>
          {isEditMode ? (
            <div className="mt-2 flex flex-wrap gap-x-8 gap-y-1 text-xs text-muted-foreground">
              {eventData.id && (
                <span className="font-mono">REF · {eventData.id}</span>
              )}
              {eventData.createdAt && (
                <span>Created · {format(eventData.createdAt, "PPP")}</span>
              )}
              {eventData.updatedAt && (
                <span>Updated · {format(eventData.updatedAt, "PPP")}</span>
              )}
            </div>
          ) : (
            <p className="max-w-xl text-sm text-muted-foreground">
              Step into the studio. Set the stage, schedule the door, and
              compose admissions.
            </p>
          )}
        </div>

        <form onSubmit={handleFormSubmit}>
          {/* 01 — Identity */}
          <Section
            index="01"
            title="Identity"
            description="The face of your event — its name as guests will know it."
          >
            <div className="space-y-2">
              <Label htmlFor="event-name" className="eyebrow !text-[0.65rem]">
                Event name
              </Label>
              <Input
                id="event-name"
                className="h-11 rounded-md border-border bg-card text-base"
                placeholder="An Evening with…"
                value={eventData.name}
                onChange={(e) => updateField("name", e.target.value)}
                required
              />
              <p className="text-xs text-muted-foreground">
                Visible to guests across the platform.
              </p>
            </div>
          </Section>

          {/* 02 — Schedule */}
          <Section
            index="02"
            title="Schedule"
            description="When doors open, and when the night closes."
          >
            <div className="space-y-3">
              <Label className="eyebrow !text-[0.65rem]">Event starts</Label>
              <DateTimeSelect
                date={eventData.startDate}
                setDate={(date) => updateField("startDate", date)}
                time={eventData.startTime}
                setTime={(time) => updateField("startTime", time)}
                enabled={eventDateEnabled}
                setEnabled={setEventDateEnabled}
              />
            </div>
            <div className="space-y-3">
              <Label className="eyebrow !text-[0.65rem]">Event ends</Label>
              <DateTimeSelect
                date={eventData.endDate}
                setDate={(date) => updateField("endDate", date)}
                time={eventData.endTime}
                setTime={(time) => updateField("endTime", time)}
                enabled={eventDateEnabled}
                setEnabled={setEventDateEnabled}
              />
            </div>
          </Section>

          {/* 03 — Venue */}
          <Section
            index="03"
            title="Venue"
            description="The room, the address, the small details guests will need."
          >
            <div className="space-y-2">
              <Label htmlFor="venue-details" className="eyebrow !text-[0.65rem]">
                Venue details
              </Label>
              <Textarea
                id="venue-details"
                className="min-h-[120px] rounded-md border-border bg-card"
                placeholder="Address, room number, entry instructions…"
                value={eventData.venueDetails}
                onChange={(e) => updateField("venueDetails", e.target.value)}
              />
            </div>
          </Section>

          {/* 04 — Sales window */}
          <Section
            index="04"
            title="Sales window"
            description="When admissions become available, and when they close."
          >
            <div className="space-y-3">
              <Label className="eyebrow !text-[0.65rem]">Sales open</Label>
              <DateTimeSelect
                date={eventData.salesStartDate}
                setDate={(date) => updateField("salesStartDate", date)}
                time={eventData.salesStartTime}
                setTime={(time) => updateField("salesStartTime", time)}
                enabled={eventSalesDateEnabled}
                setEnabled={setEventSalesDateEnabled}
              />
            </div>
            <div className="space-y-3">
              <Label className="eyebrow !text-[0.65rem]">Sales close</Label>
              <DateTimeSelect
                date={eventData.salesEndDate}
                setDate={(date) => updateField("salesEndDate", date)}
                time={eventData.salesEndTime}
                setTime={(time) => updateField("salesEndTime", time)}
                enabled={eventSalesDateEnabled}
                setEnabled={setEventSalesDateEnabled}
              />
            </div>
          </Section>

          {/* 05 — Admissions */}
          <Section
            index="05"
            title="Admissions"
            description="The tiers of entry — general, premium, patron, and beyond."
          >
            <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
              <div className="rounded-lg border border-border bg-card p-6">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2 text-sm text-foreground">
                    <Ticket className="h-4 w-4 text-gold" />
                    <span className="eyebrow">Admission tiers</span>
                  </div>
                  <Button
                    type="button"
                    onClick={handleAddTicketType}
                    variant="ghost"
                    className="h-9 cursor-pointer rounded-full border border-border bg-background px-4 text-[0.7rem] uppercase tracking-[0.22em] hover:border-gold hover:text-gold"
                  >
                    <Plus className="h-3.5 w-3.5" /> Add tier
                  </Button>
                </div>

                <div className="mt-5 space-y-2">
                  {eventData.ticketTypes.length === 0 && (
                    <div className="rounded-md border border-dashed border-border py-8 text-center text-sm text-muted-foreground">
                      No tiers yet. Add a first admission.
                    </div>
                  )}
                  {eventData.ticketTypes.map((ticketType) => (
                    <div
                      key={ticketType.id}
                      className="flex items-center justify-between rounded-md border border-border bg-background px-4 py-3"
                    >
                      <div>
                        <div className="flex items-center gap-3">
                          <p className="font-display text-base text-foreground">
                            {ticketType.name || "Untitled"}
                          </p>
                          <Badge
                            variant="outline"
                            className="border-gold/40 bg-gold/10 font-normal text-foreground"
                          >
                            ${ticketType.price}
                          </Badge>
                        </div>
                        {ticketType.totalAvailable !== undefined && (
                          <p className="mt-1 text-xs text-muted-foreground">
                            {ticketType.totalAvailable} seats
                          </p>
                        )}
                      </div>
                      <div className="flex gap-1">
                        <Button
                          type="button"
                          variant="ghost"
                          className="cursor-pointer text-muted-foreground hover:text-foreground"
                          onClick={() => handleEditTicketType(ticketType)}
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          type="button"
                          variant="ghost"
                          className="cursor-pointer text-destructive hover:text-destructive"
                          onClick={() => handleDeleteTicketType(ticketType.id)}
                        >
                          <Trash className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <DialogContent className="border-border bg-card">
                <DialogHeader>
                  <DialogTitle className="font-display text-2xl">
                    Admission tier
                  </DialogTitle>
                  <DialogDescription className="text-muted-foreground">
                    A distinct tier of entry to the event.
                  </DialogDescription>
                </DialogHeader>

                <div className="space-y-4">
                  <div className="space-y-1.5">
                    <Label htmlFor="ticket-type-name" className="eyebrow !text-[0.65rem]">
                      Name
                    </Label>
                    <Input
                      id="ticket-type-name"
                      className="h-10 rounded-md border-border bg-background"
                      placeholder="General admission, Patron, …"
                      value={currentTicketType?.name}
                      onChange={(e) =>
                        setCurrentTicketType(
                          currentTicketType
                            ? { ...currentTicketType, name: e.target.value }
                            : undefined,
                        )
                      }
                    />
                  </div>

                  <div className="flex gap-3">
                    <div className="w-full space-y-1.5">
                      <Label htmlFor="ticket-type-price" className="eyebrow !text-[0.65rem]">
                        Price (USD)
                      </Label>
                      <Input
                        id="ticket-type-price"
                        type="number"
                        className="h-10 rounded-md border-border bg-background"
                        value={currentTicketType?.price}
                        onChange={(e) =>
                          setCurrentTicketType(
                            currentTicketType
                              ? {
                                  ...currentTicketType,
                                  price: Number.parseFloat(e.target.value),
                                }
                              : undefined,
                          )
                        }
                      />
                    </div>

                    <div className="w-full space-y-1.5">
                      <Label htmlFor="ticket-type-total" className="eyebrow !text-[0.65rem]">
                        Seats
                      </Label>
                      <Input
                        id="ticket-type-total"
                        type="number"
                        className="h-10 rounded-md border-border bg-background"
                        value={currentTicketType?.totalAvailable}
                        onChange={(e) =>
                          setCurrentTicketType(
                            currentTicketType
                              ? {
                                  ...currentTicketType,
                                  totalAvailable: Number.parseFloat(
                                    e.target.value,
                                  ),
                                }
                              : undefined,
                          )
                        }
                      />
                    </div>
                  </div>

                  <div className="space-y-1.5">
                    <Label htmlFor="ticket-type-desc" className="eyebrow !text-[0.65rem]">
                      Description
                    </Label>
                    <Textarea
                      id="ticket-type-desc"
                      className="min-h-[90px] rounded-md border-border bg-background"
                      value={currentTicketType?.description}
                      onChange={(e) =>
                        setCurrentTicketType(
                          currentTicketType
                            ? {
                                ...currentTicketType,
                                description: e.target.value,
                              }
                            : undefined,
                        )
                      }
                    />
                  </div>
                </div>

                <DialogFooter>
                  <Button
                    type="button"
                    className="h-10 cursor-pointer rounded-full bg-ink px-6 text-[0.72rem] uppercase tracking-[0.22em] text-primary-foreground hover:bg-ink/90"
                    onClick={handleSaveTicketType}
                  >
                    Save tier
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </Section>

          {/* 06 — Status */}
          <Section
            index="06"
            title="Status"
            description="Keep the event private as a draft, or publish for guests."
          >
            <div className="space-y-2">
              <Label className="eyebrow !text-[0.65rem]">Visibility</Label>
              <Select
                value={eventData.status}
                onValueChange={(value) => updateField("status", value)}
              >
                <SelectTrigger className="h-11 w-[220px] rounded-md border-border bg-card text-foreground">
                  <SelectValue placeholder="Select status" />
                </SelectTrigger>
                <SelectContent className="border-border bg-card text-foreground">
                  <SelectItem value={EventStatusEnum.DRAFT}>Draft</SelectItem>
                  <SelectItem value={EventStatusEnum.PUBLISHED}>
                    Published
                  </SelectItem>
                  <SelectItem value={EventStatusEnum.CANCELLED}>
                    Cancelled
                  </SelectItem>
                  <SelectItem value={EventStatusEnum.COMPLETED}>
                    Completed
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
          </Section>

          {error && (
            <Alert
              variant="destructive"
              className="my-6 border-destructive/40 bg-card"
            >
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          <div className="flex flex-col-reverse items-center gap-4 border-t border-border pt-10 md:flex-row md:justify-between">
            <Button
              type="button"
              variant="ghost"
              className="h-11 cursor-pointer rounded-full text-[0.72rem] uppercase tracking-[0.22em] text-muted-foreground hover:text-foreground"
              onClick={() => navigate("/dashboard/events")}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              onClick={handleFormSubmit}
              className="h-12 cursor-pointer rounded-full bg-ink px-10 text-[0.74rem] uppercase tracking-[0.24em] text-primary-foreground hover:bg-ink/90"
            >
              {isEditMode ? "Save changes" : "Compose event"}
            </Button>
          </div>
        </form>
      </main>
    </div>
  );
};

export default DashboardManageEventPage;

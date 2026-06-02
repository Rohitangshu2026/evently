import {
  CreateEventRequest,
  EventDetails,
  EventSummary,
  isErrorResponse,
  PublishedEventDetails,
  PublishedEventSummary,
  SpringBootPagination,
  TicketDetails,
  TicketSummary,
  TicketValidationRequest,
  TicketValidationResponse,
  UpdateEventRequest,
} from "@/domain/domain";

const friendlyStatus = (status: number): string => {
  switch (status) {
    case 400:
      return "The request was malformed.";
    case 401:
      return "You need to sign in to continue.";
    case 403:
      return "You don't have permission to perform that action.";
    case 404:
      return "We couldn't find what you were looking for.";
    case 409:
      return "That action conflicts with the current state.";
    case 500:
      return "The server ran into a problem. Please try again.";
    case 502:
    case 503:
    case 504:
      return "The server is unavailable right now. Please try again shortly.";
    default:
      return `Request failed (HTTP ${status}).`;
  }
};

// Parse a fetch response carefully. Handles:
//   - empty body (no JSON)
//   - non-JSON body (HTML error page)
//   - JSON body
// Always throws a clean Error for non-OK responses.
async function handle<T>(response: Response, expectJson = true): Promise<T> {
  const text = await response.text();
  let body: unknown = null;
  if (text.length > 0) {
    try {
      body = JSON.parse(text);
    } catch {
      body = text;
    }
  }

  if (!response.ok) {
    if (isErrorResponse(body)) {
      throw new Error(body.error);
    }
    if (typeof body === "string" && body.length > 0 && body.length < 200) {
      throw new Error(body);
    }
    throw new Error(friendlyStatus(response.status));
  }

  if (!expectJson) {
    return undefined as T;
  }
  return body as T;
}

const authHeaders = (accessToken: string) => ({
  Authorization: `Bearer ${accessToken}`,
  "Content-Type": "application/json",
});

const jsonHeaders = { "Content-Type": "application/json" };

export const createEvent = async (
  accessToken: string,
  request: CreateEventRequest,
): Promise<void> => {
  const response = await fetch("/api/v1/events", {
    method: "POST",
    headers: authHeaders(accessToken),
    body: JSON.stringify(request),
  });
  await handle<void>(response, false);
};

export const updateEvent = async (
  accessToken: string,
  id: string,
  request: UpdateEventRequest,
): Promise<void> => {
  const response = await fetch(`/api/v1/events/${id}`, {
    method: "PUT",
    headers: authHeaders(accessToken),
    body: JSON.stringify(request),
  });
  await handle<void>(response, false);
};

export const listEvents = async (
  accessToken: string,
  page: number,
): Promise<SpringBootPagination<EventSummary>> => {
  const response = await fetch(`/api/v1/events?page=${page}&size=2`, {
    method: "GET",
    headers: authHeaders(accessToken),
  });
  return handle<SpringBootPagination<EventSummary>>(response);
};

export const getEvent = async (
  accessToken: string,
  id: string,
): Promise<EventDetails> => {
  const response = await fetch(`/api/v1/events/${id}`, {
    method: "GET",
    headers: authHeaders(accessToken),
  });
  return handle<EventDetails>(response);
};

export const deleteEvent = async (
  accessToken: string,
  id: string,
): Promise<void> => {
  const response = await fetch(`/api/v1/events/${id}`, {
    method: "DELETE",
    headers: authHeaders(accessToken),
  });
  await handle<void>(response, false);
};

export const listPublishedEvents = async (
  page: number,
): Promise<SpringBootPagination<PublishedEventSummary>> => {
  const response = await fetch(
    `/api/v1/published-events?page=${page}&size=4`,
    {
      method: "GET",
      headers: jsonHeaders,
    },
  );
  return handle<SpringBootPagination<PublishedEventSummary>>(response);
};

export const searchPublishedEvents = async (
  query: string,
  page: number,
): Promise<SpringBootPagination<PublishedEventSummary>> => {
  const response = await fetch(
    `/api/v1/published-events?q=${encodeURIComponent(query)}&page=${page}&size=4`,
    {
      method: "GET",
      headers: jsonHeaders,
    },
  );
  return handle<SpringBootPagination<PublishedEventSummary>>(response);
};

export const getPublishedEvent = async (
  id: string,
): Promise<PublishedEventDetails> => {
  const response = await fetch(`/api/v1/published-events/${id}`, {
    method: "GET",
    headers: jsonHeaders,
  });
  return handle<PublishedEventDetails>(response);
};

export const purchaseTicket = async (
  accessToken: string,
  eventId: string,
  ticketTypeId: string,
): Promise<void> => {
  const response = await fetch(
    `/api/v1/events/${eventId}/ticket-types/${ticketTypeId}/tickets`,
    {
      method: "POST",
      headers: authHeaders(accessToken),
    },
  );
  await handle<void>(response, false);
};

export const listTickets = async (
  accessToken: string,
  page: number,
): Promise<SpringBootPagination<TicketSummary>> => {
  const response = await fetch(`/api/v1/tickets?page=${page}&size=8`, {
    method: "GET",
    headers: authHeaders(accessToken),
  });
  return handle<SpringBootPagination<TicketSummary>>(response);
};

export const getTicket = async (
  accessToken: string,
  id: string,
): Promise<TicketDetails> => {
  const response = await fetch(`/api/v1/tickets/${id}`, {
    method: "GET",
    headers: authHeaders(accessToken),
  });
  return handle<TicketDetails>(response);
};

export const getTicketQr = async (
  accessToken: string,
  id: string,
): Promise<Blob> => {
  const response = await fetch(`/api/v1/tickets/${id}/qr-codes`, {
    method: "GET",
    headers: {
      Authorization: `Bearer ${accessToken}`,
    },
  });

  if (!response.ok) {
    throw new Error(friendlyStatus(response.status));
  }
  return response.blob();
};

export const validateTicket = async (
  accessToken: string,
  request: TicketValidationRequest,
): Promise<TicketValidationResponse> => {
  const response = await fetch(`/api/v1/ticket-validations`, {
    method: "POST",
    headers: authHeaders(accessToken),
    body: JSON.stringify(request),
  });
  return handle<TicketValidationResponse>(response);
};

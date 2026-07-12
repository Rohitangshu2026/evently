/**
 * Role flags for the signed-in user. Roles arrive in the auth responses as
 * plain strings (ORGANIZER / ATTENDEE / STAFF), so no token decoding is
 * needed — this is a thin projection over the auth context.
 */
import { useAuth } from "@/lib/auth";

interface UseRolesReturn {
  isLoading: boolean;
  roles: string[];
  isOrganizer: boolean;
  isAttendee: boolean;
  isStaff: boolean;
}

export const useRoles = (): UseRolesReturn => {
  const { isLoading, user } = useAuth();
  const roles = user?.roles ?? [];

  return {
    isLoading,
    roles,
    isOrganizer: roles.includes("ORGANIZER"),
    isAttendee: roles.includes("ATTENDEE"),
    isStaff: roles.includes("STAFF"),
  };
};

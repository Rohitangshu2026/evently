/**
 * Custom authentication for the Evently API — replaces react-oidc-context.
 *
 * The backend is its own identity provider: short-lived access tokens come
 * back from /auth/login|signup|refresh, while the long-lived refresh token
 * only ever travels in an httpOnly cookie the browser attaches to /auth
 * calls automatically. The access token is held in memory (never storage),
 * and a timer silently refreshes it shortly before expiry. On a hard reload
 * the provider restores the session with one /auth/refresh call.
 *
 * The exposed `useAuth()` surface deliberately mirrors the parts of
 * react-oidc-context this app consumed (`isLoading`, `isAuthenticated`,
 * `user.access_token`, `user.profile`, `signinRedirect`, `signoutRedirect`),
 * so swapping providers was an import change for consumers rather than a
 * rewrite.
 */
import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
} from "react";
import { isErrorResponse } from "@/domain/domain";

/** The user object returned by the auth endpoints. */
interface ApiUser {
  id: string;
  email: string;
  name: string;
  roles: string[];
}

/** Body of a successful signup/login/refresh response. */
interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: ApiUser;
}

/**
 * Authenticated user as seen by the app. `access_token` and `profile` keep
 * the field names the previous OIDC library used, so existing pages read
 * them unchanged; `roles` comes straight from the API response.
 */
export interface AuthUser {
  access_token: string;
  roles: string[];
  profile: {
    preferred_username: string;
    email: string;
  };
}

interface AuthContextValue {
  isLoading: boolean;
  isAuthenticated: boolean;
  user: AuthUser | null;
  /** Authenticates with email/password. Throws with a message on failure. */
  signIn: (email: string, password: string) => Promise<void>;
  /** Registers a new account and signs it in. Throws with a message on failure. */
  signUp: (
    email: string,
    password: string,
    name: string,
    role: string,
  ) => Promise<void>;
  /** Compat with the old OIDC surface: sends the visitor to the login page. */
  signinRedirect: () => void;
  /** Revokes the session server-side, clears state, returns to the home page. */
  signoutRedirect: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/** Refresh this many seconds before the access token would expire. */
const REFRESH_MARGIN_SECONDS = 60;

const toAuthUser = (response: AuthResponse): AuthUser => ({
  access_token: response.accessToken,
  roles: response.user.roles,
  profile: {
    preferred_username: response.user.name,
    email: response.user.email,
  },
});

/** Calls an auth endpoint and parses the standard response/error shapes. */
async function authRequest(
  path: string,
  body?: Record<string, string>,
): Promise<AuthResponse> {
  const response = await fetch(`/api/v1/auth/${path}`, {
    method: "POST",
    headers: body ? { "Content-Type": "application/json" } : undefined,
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  let parsed: unknown = null;
  if (text.length > 0) {
    try {
      parsed = JSON.parse(text);
    } catch {
      parsed = null;
    }
  }

  if (!response.ok) {
    if (isErrorResponse(parsed)) {
      throw new Error(parsed.error);
    }
    throw new Error("Something went wrong. Please try again.");
  }
  return parsed as AuthResponse;
}

export const AuthProvider = ({ children }: { children: ReactNode }) => {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const refreshTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  const clearRefreshTimer = () => {
    if (refreshTimer.current) {
      clearTimeout(refreshTimer.current);
      refreshTimer.current = null;
    }
  };

  /** Stores a session and arms the silent-refresh timer. */
  const adoptSession = useCallback((response: AuthResponse) => {
    setUser(toAuthUser(response));
    clearRefreshTimer();
    const delaySeconds = Math.max(
      response.expiresIn - REFRESH_MARGIN_SECONDS,
      30,
    );
    refreshTimer.current = setTimeout(async () => {
      try {
        adoptSession(await authRequest("refresh"));
      } catch {
        setUser(null);
      }
    }, delaySeconds * 1000);
  }, []);

  // One refresh attempt on mount restores the session across hard reloads
  // (the refresh cookie survives; the in-memory access token does not).
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await authRequest("refresh");
        if (!cancelled) adoptSession(response);
      } catch {
        // No live session — that's a normal signed-out state.
      } finally {
        if (!cancelled) setIsLoading(false);
      }
    })();
    return () => {
      cancelled = true;
      clearRefreshTimer();
    };
  }, [adoptSession]);

  const signIn = useCallback(
    async (email: string, password: string) => {
      adoptSession(await authRequest("login", { email, password }));
    },
    [adoptSession],
  );

  const signUp = useCallback(
    async (email: string, password: string, name: string, role: string) => {
      adoptSession(await authRequest("signup", { email, password, name, role }));
    },
    [adoptSession],
  );

  const signinRedirect = useCallback(() => {
    globalThis.location.assign("/login");
  }, []);

  const signoutRedirect = useCallback(async () => {
    try {
      await fetch("/api/v1/auth/logout", { method: "POST" });
    } catch {
      // Local sign-out proceeds even if the network call fails.
    }
    clearRefreshTimer();
    setUser(null);
    globalThis.location.assign("/");
  }, []);

  return (
    <AuthContext.Provider
      value={{
        isLoading,
        isAuthenticated: user !== null,
        user,
        signIn,
        signUp,
        signinRedirect,
        signoutRedirect,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

/**
 * Access the auth session. Must be used under {@link AuthProvider}.
 */
// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = (): AuthContextValue => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};

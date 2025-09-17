# Spring Security: Unauthenticated Request Flow (Login Redirect)

This document details the step-by-step flow of an HTTP request from an **unauthenticated user** for a protected resource, resulting in a redirect to the login page.

---

## Part 1: Conceptual Flow (The Challenge)

When an unauthenticated user requests a protected resource, the primary goal of the Spring Security filter chain is not to grant access, but to initiate an authentication challenge. Here is how it happens.

Each filter is presented as **`Default Implementing Class`** (`Interface`).

### 1. `SecurityContextHolderFilter` (`jakarta.servlet.Filter`)

This filter runs at the beginning of the request.

*   **Role:** It attempts to load a `SecurityContext` from the `SecurityContextRepository`. For a new, unauthenticated user, the repository (e.g., `HttpSession`) will be empty. The `SecurityContextHolder` will therefore remain empty or be populated with a default, empty context.

### 2. `AnonymousAuthenticationFilter` (`jakarta.servlet.Filter`)

This filter provides a fallback identity.

*   **Role:** It checks the `SecurityContextHolder`. Since the context is empty, this filter creates an `AnonymousAuthenticationToken` and places it in the context. At this point, the request is considered "authenticated" as an anonymous user.

### 3. `AuthorizationFilter` (`jakarta.servlet.Filter`)

This is where the access rule is enforced.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.access.intercept.AuthorizationFilter`
*   **Role:**
    1.  It gets the current `Authentication` object from the `SecurityContextHolder`, which is the `AnonymousAuthenticationToken`.
    2.  It checks the authorization rules for the requested URL (e.g., must have `ROLE_USER`).
    3.  The `AuthorizationManager` determines that the anonymous user does not have the required role and throws an **`AccessDeniedException`**.

### 4. `ExceptionTranslationFilter` (`jakarta.servlet.Filter`)

This filter is the key to handling the security exception and starting the login process.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.access.ExceptionTranslationFilter`
*   **Role:**
    1.  It catches the `AccessDeniedException` thrown by the `AuthorizationFilter`.
    2.  It checks the state of the current user. Since the user is anonymous (not fully authenticated), it determines that it must **Start Authentication**.
    3.  It clears the `SecurityContextHolder`.
    4.  It saves the original request (e.g., `GET /protected-resource`) into a `RequestCache` so the user can be redirected back to it after a successful login.
    5.  It delegates to a configured `AuthenticationEntryPoint` to "challenge" the user for credentials.

### 5. `AuthenticationEntryPoint` (`org.springframework.security.web.AuthenticationEntryPoint`)

This interface is responsible for sending the actual challenge to the client.

*   **Interface:** `org.springframework.security.web.AuthenticationEntryPoint`
*   **Default Implementing Class for Form Login:** `org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint`
*   **Role:** The `commence()` method is called by the `ExceptionTranslationFilter`. The `LoginUrlAuthenticationEntryPoint` implementation sends an **HTTP 302 Redirect** response to the client, telling the browser to navigate to the login page (e.g., `/login`).

The request processing stops here. The client's browser will now make a new request to the `/login` page, and the authentication flow (as documented in `spring_security_authentication_flow.md`) will begin.

---

## Part 2: Mermaid Diagram of the Unauthenticated Flow

This diagram visualizes how an unauthenticated request for a protected resource is handled.

```mermaid
sequenceDiagram
    actor Client
    participant FCP as FilterChainProxy
    participant AF as AuthorizationFilter
    participant ETF as ExceptionTranslationFilter
    participant AEP as "<<interface>> <br> AuthenticationEntryPoint"

    %% Define styles for classes and interfaces
    classDef interface fill:#E8F3FD,stroke:#3471A5,stroke-width:2px;
    classDef class fill:#FFF2CC,stroke:#D6B656,stroke-width:2px;

    %% Apply styles
    class AEP interface;
    class FCP,AF,ETF class;

    Client->>FCP: 1. GET /protected-resource
    activate FCP

    FCP->>ETF: 2. doFilter()
    activate ETF

    ETF->>AF: 3. doFilter()
    activate AF
    note right of AF: User is anonymous, access is denied
    AF-->>ETF: 4. throws AccessDeniedException
    deactivate AF

    note right of ETF: Catches exception, determines user is anonymous
    ETF->>AEP: 5. commence()
    activate AEP
    note right of AEP: Impl: LoginUrlAuthenticationEntryPoint
    AEP-->>ETF: 6. Prepares redirect response
    deactivate AEP

    ETF-->>FCP: 7. Returns redirect response
    deactivate ETF

    FCP-->>Client: 8. HTTP 302 Redirect to /login
    deactivate FCP
```

# Spring Security: Authenticated Request Flow

This document details the step-by-step flow of an HTTP request from an **already authenticated** user through the default Spring Security filter chain until it reaches the application's controller.

This flow assumes the user has already logged in via a form, and their `SecurityContext` is persisted, typically in the `HttpSession`.

---

## Part 1: Conceptual Flow (The Filter Chain)

When a request arrives, it passes through a chain of filters, each with a specific responsibility. Below are the key filters from the default chain that participate in processing a standard request from an authenticated user, in the order they are invoked.

Each filter is presented as **`Default Implementing Class`** (`Interface`).

### 1. `SecurityContextHolderFilter` (`jakarta.servlet.Filter`)

This is one of the first critical filters in the chain for an authenticated user.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.context.SecurityContextHolderFilter`
*   **Role:**
    1.  It interacts with a configured `SecurityContextRepository` (by default, the `HttpSessionSecurityContextRepository`).
    2.  It **loads** the `SecurityContext` (containing the user's `Authentication` object) from the repository (e.g., the `HttpSession`).
    3.  It populates the `SecurityContextHolder` with this `SecurityContext`, making it available to the rest of the application for the duration of the request.
    4.  After the rest of the filter chain and the controller have executed, this filter **clears** the `SecurityContextHolder`.

### 2. `HeaderWriterFilter` (`jakarta.servlet.Filter`)

This filter's job is to add various security-related headers to the HTTP response.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.header.HeaderWriterFilter`
*   **Role:** It adds headers like `X-Content-Type-Options`, `X-XSS-Protection`, and `X-Frame-Options` to the response to help mitigate common web vulnerabilities. It does its main work on the way back out of the filter chain.

### 3. `CsrfFilter` (`jakarta.servlet.Filter`)

This filter provides protection against Cross-Site Request Forgery (CSRF) attacks.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.csrf.CsrfFilter`
*   **Role:**
    *   For state-changing HTTP methods (like `POST`, `PUT`, `DELETE`), it checks for the presence and validity of a CSRF token in the request. If the token is missing or invalid, it denies the request.
    *   For `GET` requests, it typically does not block but may make a new CSRF token available to the response.

### 4. `LogoutFilter` (`jakarta.servlet.Filter`)

This filter watches for requests that signify a user wishes to log out.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.authentication.logout.LogoutFilter`
*   **Role:** It listens for a specific URL (by default, `/logout`). If the incoming request matches this URL, the filter handles the logout process (clearing the session, invalidating the security context, etc.). For any other URL, it does nothing and simply passes the request down the chain.

### 5. `RequestCacheAwareFilter` (`jakarta.servlet.Filter`)

This filter is responsible for replaying a request that was saved before authentication.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.savedrequest.RequestCacheAwareFilter`
*   **Role:** After a user logs in, this filter checks the `RequestCache` (e.g., `HttpSessionRequestCache`) to see if there was an original request that was interrupted for authentication. If so, it redirects the user to that original URL. For a normal request made by an already-authenticated user, there is no saved request, so this filter does nothing.

### 6. `SecurityContextHolderAwareRequestFilter` (`jakarta.servlet.Filter`)

This filter enriches the `HttpServletRequest` object itself.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter`
*   **Role:** It wraps the `HttpServletRequest` into a security-aware version. This allows standard Servlet API methods like `request.getRemoteUser()` and `request.isUserInRole()` to work as expected, pulling data from the `SecurityContextHolder`.

### 7. `AnonymousAuthenticationFilter` (`jakarta.servlet.Filter`)

This filter provides a default, anonymous identity if no other identity is present.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.authentication.AnonymousAuthenticationFilter`
*   **Role:** It inspects the `SecurityContextHolder`. If the context is empty (i.e., no `Authentication` object), it creates an `AnonymousAuthenticationToken` and populates the context with it. In the flow for an *authenticated* user, the context is already populated by `SecurityContextHolderFilter`, so this filter does nothing.

### 8. `ExceptionTranslationFilter` (`jakarta.servlet.Filter`)

This is a crucial filter for handling security-related exceptions that occur further down the chain.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.access.ExceptionTranslationFilter`
*   **Role:** It doesn't perform any security checks itself. Instead, it wraps its `doFilter` call in a `try...catch` block. Its primary purpose in this flow is to catch `AccessDeniedException`. If `AuthorizationFilter` denies access, this filter will catch the exception and delegate to an `AccessDeniedHandler`, which typically returns an HTTP `403 Forbidden` response.

### 9. `AuthorizationFilter` (`jakarta.servlet.Filter`)

This is the filter that performs the actual authorization check.

*   **Interface:** `jakarta.servlet.Filter`
*   **Default Implementing Class:** `org.springframework.security.web.access.intercept.AuthorizationFilter`
*   **Role:**
    1.  It obtains the `Authentication` object from the `SecurityContextHolder`.
    2.  It gets the configuration attributes (e.g., required roles like `ROLE_ADMIN`) for the requested URL from an `AuthorizationManager`.
    3.  It passes the `Authentication` object and the configuration attributes to the `AuthorizationManager` to decide if access should be granted.
    4.  If access is denied, the manager throws an `AccessDeniedException`, which is then handled by the `ExceptionTranslationFilter`.
    5.  If access is granted, it simply allows the request to proceed to the next filter in the chain (and ultimately to the controller).

### 10. Destination: The Controller

If the request successfully passes through all the preceding filters without being denied or redirected, it is finally dispatched to the `DispatcherServlet`, which routes it to the appropriate `@Controller` or `@RestController` method for processing.

---

## Part 2: Mermaid Diagram of the Authenticated Flow

This diagram visualizes the path of a successful request from an authenticated user.

```mermaid
sequenceDiagram
    actor Client
    participant FCP as FilterChainProxy
    participant SCHF as SecurityContextHolderFilter
    participant SCR as "<<interface>> <br> SecurityContextRepository"
    participant AF as AuthorizationFilter
    participant AM as "<<interface>> <br> AuthorizationManager"
    participant ETF as ExceptionTranslationFilter
    participant DS as DispatcherServlet

    %% Define styles for classes and interfaces
    classDef interface fill:#E8F3FD,stroke:#3471A5,stroke-width:2px;
    classDef class fill:#FFF2CC,stroke:#D6B656,stroke-width:2px;

    %% Apply styles
    class SCR,AM interface;
    class FCP,SCHF,AF,ETF,DS class;

    Client->>FCP: GET /api/resource
    activate FCP

    FCP->>SCHF: doFilter()
    activate SCHF
    SCHF->>SCR: loadContext()
    activate SCR
    note right of SCR: Impl: HttpSessionSecurityContextRepository
    SCR-->>SCHF: returns SecurityContext
    deactivate SCR
    SCHF->>SCHF: Populates SecurityContextHolder
    deactivate SCHF

    FCP->>ETF: doFilter()
    activate ETF
    note right of ETF: Wraps next calls in try...catch

    FCP->>AF: doFilter()
    activate AF
    AF->>AF: Gets Authentication from SecurityContextHolder
    AF->>AM: check(authentication, request)
    activate AM
    AM-->>AF: returns AuthorizationDecision(granted=true)
    deactivate AM
    deactivate AF

    FCP->>DS: doFilter()
    activate DS
    DS->>DS: Routes to Controller
    DS-->>FCP: returns response
    deactivate DS

    deactivate ETF
    deactivate FCP
    FCP-->>Client: HTTP 200 OK with response
```

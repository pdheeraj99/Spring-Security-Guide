# Spring Security: Username & Password Authentication Flow

This document provides a detailed, step-by-step breakdown of the Servlet-based Username and Password authentication mechanism in Spring Security. The flow covers every major class and interface from the initial HTTP request to the point where a `SecurityContext` is established for the user.

## Core Architectural Components

Before diving into the flow, it's essential to understand the key architectural components that form the foundation of Spring Security's authentication process.

| Interface/Class             | Description                                                                                                                              |
| --------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| `SecurityContextHolder`     | The central container for security information, typically using a `ThreadLocal` to store the `SecurityContext`.                          |
| `SecurityContext`           | Holds the `Authentication` object for the currently authenticated user.                                                                  |
| `Authentication`            | Represents the user's authentication token. It holds the principal, credentials, and granted authorities.                                |
| `GrantedAuthority`          | Represents a single permission or role granted to the principal (e.g., `ROLE_USER`).                                                     |
| `AuthenticationManager`     | The primary service interface for authenticating a user.                                                                                 |
| `ProviderManager`           | The default implementation of `AuthenticationManager`, which delegates to a list of `AuthenticationProvider`s.                           |
| `AuthenticationProvider`    | Responsible for a specific authentication strategy (e.g., `DaoAuthenticationProvider` for username/password).                              |
| `UserDetails`               | Represents the core user information (username, password, authorities, account status).                                                  |
| `UserDetailsService`        | An interface for loading `UserDetails` from a persistent store.                                                                          |
| `PasswordEncoder`           | An interface for encoding and validating passwords.                                                                                      |
| `SecurityFilterChain`       | A chain of servlet filters that are applied to an HTTP request.                                                                          |
| `AuthenticationEntryPoint`  | An interface used to commence the authentication process when an unauthenticated user tries to access a protected resource.              |

---

## The End-to-End Authentication Flow

The following steps describe the complete process for a form-based login scenario.

### Step 1: An Unauthenticated User Makes a Request

The process begins when a user's browser sends an HTTP request for a resource that requires authentication.

1.  **`FilterChainProxy`**: This is the main entry point for Spring Security in the servlet filter chain. It delegates the request to a specific `SecurityFilterChain` that matches the request URL.
2.  **`SecurityContextHolderFilter`**: This filter is responsible for populating the `SecurityContextHolder` with a `SecurityContext` from a `SecurityContextRepository` at the beginning of the request, and clearing it at the end. For an unauthenticated user, the context will be empty or contain an anonymous authentication token.
3.  **`AuthorizationFilter`**: This filter (and others like it) determines if the current user is authorized to access the requested resource. Since the user is not authenticated, it throws an `AccessDeniedException`.

**Key Interfaces & Classes:**

*   **Filter:** `jakarta.servlet.Filter`
    *   **Implementations:** `org.springframework.web.filter.DelegatingFilterProxy`, `org.springframework.security.web.FilterChainProxy`, `org.springframework.security.web.context.SecurityContextHolderFilter`, `org.springframework.security.web.access.intercept.AuthorizationFilter`.
*   **Security Context Repository:** `org.springframework.security.web.context.SecurityContextRepository`
    *   **Implementation:** `org.springframework.security.web.context.HttpSessionSecurityContextRepository` (default, stores context in the HTTP session).

### Step 2: Commencing Authentication - Redirect to Login Form

The `AccessDeniedException` is caught by a filter designed to handle it and initiate authentication.

1.  **`ExceptionTranslationFilter`**: This filter catches security-related exceptions. Since the user is unauthenticated, it does not re-throw the `AccessDeniedException`. Instead, it initiates the "Start Authentication" process by consulting its configured `AuthenticationEntryPoint`.
2.  **`AuthenticationEntryPoint`**: This interface is responsible for starting the authentication scheme.
    *   **Interface:** `org.springframework.security.web.AuthenticationEntryPoint`
    *   **Form Login Implementation:** `org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint`. This implementation saves the current request (so the user can be redirected back to it after a successful login) and sends a `302 Found` redirect response to the client, pointing to the configured login page URL (e.g., `/login`).

### Step 3: Processing the Login Form Submission

The user provides their credentials in the login form and submits it. A specific filter is responsible for processing this submission.

1.  **`UsernamePasswordAuthenticationFilter`**: This filter extends `AbstractAuthenticationProcessingFilter` and is configured to intercept POST requests to the login URL (e.g., `/login`).
2.  **Token Creation**: The filter extracts the username and password from the `HttpServletRequest` parameters.
3.  **`Authentication` Object**: It uses these credentials to create an *unauthenticated* `UsernamePasswordAuthenticationToken`. This object holds the user-provided credentials and implements the `Authentication` interface.
4.  **Delegation**: The filter then passes this `Authentication` token to the `AuthenticationManager` for validation.

**Key Interfaces & Classes:**

*   **Filter:** `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter`
    *   **Base Class:** `org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter`
*   **Authentication Token:** `org.springframework.security.authentication.UsernamePasswordAuthenticationToken`
    *   **Interface:** `org.springframework.security.core.Authentication`

### Step 4: The Core Authentication Logic

This is where the user's credentials are actually validated.

1.  **`AuthenticationManager`**: The `UsernamePasswordAuthenticationFilter` invokes the `authenticate()` method on this manager.
    *   **Interface:** `org.springframework.security.authentication.AuthenticationManager`
    *   **Default Implementation:** `org.springframework.security.authentication.ProviderManager`.
2.  **`ProviderManager`**: This implementation iterates through a list of configured `AuthenticationProvider`s. It asks each provider if it `supports()` the type of the incoming `Authentication` token (in this case, `UsernamePasswordAuthenticationToken`).
3.  **`DaoAuthenticationProvider`**: This is the `AuthenticationProvider` that supports `UsernamePasswordAuthenticationToken`. It's responsible for the core username/password validation logic.
    *   **Class:** `org.springframework.security.authentication.dao.DaoAuthenticationProvider`
    *   **a. Retrieve User**: It calls the `loadUserByUsername()` method on its configured `UserDetailsService`.
    *   **b. Validate Password**: It uses its configured `PasswordEncoder` to compare the plaintext password from the `UsernamePasswordAuthenticationToken` with the hashed password from the `UserDetails` object returned by the `UserDetailsService`.

**Key Interfaces & Classes:**

*   **User Details Service:** `org.springframework.security.core.userdetails.UserDetailsService`
    *   **Implementations:**
        *   `org.springframework.security.provisioning.InMemoryUserDetailsManager`: Stores users in memory.
        *   `org.springframework.security.provisioning.JdbcUserDetailsManager`: Stores users in a database via JDBC.
        *   Custom application-specific implementations are very common.
*   **User Details:** `org.springframework.security.core.userdetails.UserDetails`
    *   **Common Implementation:** `org.springframework.security.core.userdetails.User`.
*   **Password Encoder:** `org.springframework.security.crypto.password.PasswordEncoder`
    *   **Implementations:**
        *   `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder` (Recommended standard).
        *   `org.springframework.security.crypto.argon2.Argon2PasswordEncoder`.
        *   `org.springframework.security.crypto.password.DelegatingPasswordEncoder` (Default, allows for multiple encoding schemes).

### Step 5: Handling a Successful Authentication

If the password is correct and the user's account is valid (not locked, disabled, or expired), the authentication succeeds.

1.  **Authenticated Token**: The `DaoAuthenticationProvider` creates a *new*, fully authenticated `UsernamePasswordAuthenticationToken`. This token contains the `UserDetails` object as its principal, the user's `GrantedAuthority`s, and its `authenticated` flag is set to `true`. The original password credential is erased for security.
2.  **Return to Filter**: The `ProviderManager` returns this authenticated token to the `UsernamePasswordAuthenticationFilter`.
3.  **`SecurityContext` Update**: The filter sets this `Authentication` object on the `SecurityContext`: `SecurityContextHolder.getContext().setAuthentication(authentication)`.
4.  **Persistence**: The `SecurityContextHolderFilter` (via the `SecurityContextRepository`) saves the `SecurityContext` (e.g., to the `HttpSession`) so it's available for subsequent requests.
5.  **Success Handler**: The filter invokes the configured `AuthenticationSuccessHandler`.
    *   **Interface:** `org.springframework.security.web.authentication.AuthenticationSuccessHandler`
    *   **Default Implementation:** `org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler`. This handler redirects the user to the URL they originally tried to access, which was saved in Step 2.

### Step 6: Handling a Failed Authentication

If the username is not found or the password does not match, an `AuthenticationException` is thrown.

1.  **Exception**: The `DaoAuthenticationProvider` throws a subtype of `AuthenticationException` (e.g., `BadCredentialsException` or `UsernameNotFoundException`).
2.  **Return to Filter**: The `ProviderManager` propagates this exception up to the `UsernamePasswordAuthenticationFilter`.
3.  **Clear Context**: The filter ensures the `SecurityContextHolder` is cleared.
4.  **Failure Handler**: The filter invokes the configured `AuthenticationFailureHandler`.
    *   **Interface:** `org.springframework.security.web.authentication.AuthenticationFailureHandler`
    *   **Default Implementation:** `org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler`. This handler redirects the user back to the login page, usually with an error flag in the URL (e.g., `/login?error`).

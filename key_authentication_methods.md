# Key Methods in Spring Security's Authentication Flow

This document lists the single, most critical method for each major component involved in the username & password authentication flow.

---

### 1. `org.springframework.security.web.AuthenticationEntryPoint`

This interface is used to initiate the authentication process when an unauthenticated user attempts to access a protected resource.

*   **Key Method:** `commence()`
*   **Signature:** `void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)`
*   **Role:** Called by `ExceptionTranslationFilter`. Its job is to "ask" the client for credentials. In a form-based login flow, the `LoginUrlAuthenticationEntryPoint` implementation performs a redirect to the login page.

### 2. `org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter`

This filter intercepts the POST request to the login URL and orchestrates the authentication attempt.

*   **Key Method:** `attemptAuthentication()`
*   **Signature:** `public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)`
*   **Role:** Extracts the username and password from the request, creates a `UsernamePasswordAuthenticationToken`, and passes it to the `AuthenticationManager` for validation.

### 3. `org.springframework.security.authentication.AuthenticationManager`

This is the main service interface for performing authentication.

*   **Key Method:** `authenticate()`
*   **Signature:** `Authentication authenticate(Authentication authentication)`
*   **Role:** Takes a partially filled `Authentication` object (containing user credentials) and returns a fully authenticated object if successful, or throws an `AuthenticationException` if not. The `ProviderManager` is the standard implementation.

### 4. `org.springframework.security.authentication.dao.DaoAuthenticationProvider`

This is the `AuthenticationProvider` implementation that handles username/password authentication.

*   **Key Method:** `authenticate()`
*   **Signature:** `public Authentication authenticate(Authentication authentication)`
*   **Role:** Retrieves user details from a `UserDetailsService` and validates the provided password using a `PasswordEncoder`.

### 5. `org.springframework.security.core.userdetails.UserDetailsService`

An interface for retrieving user-related data.

*   **Key Method:** `loadUserByUsername()`
*   **Signature:** `UserDetails loadUserByUsername(String username)`
*   **Role:** Locates the user based on the username. The returned `UserDetails` object contains the stored (encoded) password and other user details.

### 6. `org.springframework.security.crypto.password.PasswordEncoder`

An interface for encoding and verifying passwords.

*   **Key Method:** `matches()`
*   **Signature:** `boolean matches(CharSequence rawPassword, String encodedPassword)`
*   **Role:** Verifies that the raw password provided by the user matches the encoded password retrieved from the data store.

### 7. `org.springframework.security.web.authentication.AuthenticationSuccessHandler`

Defines the strategy for handling a successful authentication.

*   **Key Method:** `onAuthenticationSuccess()`
*   **Signature:** `void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)`
*   **Role:** Called after a user is successfully authenticated. The default implementation (`SavedRequestAwareAuthenticationSuccessHandler`) redirects the user to the page they were originally trying to access.

### 8. `org.springframework.security.web.authentication.AuthenticationFailureHandler`

Defines the strategy for handling a failed authentication.

*   **Key Method:** `onAuthenticationFailure()`
*   **Signature:** `void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)`
*   **Role:** Called after an authentication attempt fails. The default implementation (`SimpleUrlAuthenticationFailureHandler`) redirects the user back to the login page with an error flag.

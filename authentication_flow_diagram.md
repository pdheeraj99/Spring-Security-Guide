# Spring Security Authentication Flow Diagram

This document contains a Mermaid sequence diagram that visualizes the entire Spring Security username & password authentication flow.

To render the diagram, you can copy the code block below and paste it into any Mermaid-compatible viewer (like the Mermaid Live Editor, or plugins in IDEs like VS Code).

## The Diagram

### How to read the diagram:

*   **Blue boxes with `<<interface>>`**: These represent Java interfaces. They define a contract, and the actual object used at runtime is a concrete implementation of that interface.
*   **Yellow boxes**: These represent concrete Java classes that are part of the framework.
*   **Dotted lines**: These represent return values or replies.
*   **Solid lines**: These represent method calls or requests.
*   **Boxes with `title`**: The flow is grouped into two main parts to make it easier to follow.
*   **Notes**: The yellow "sticky notes" provide extra context, such as the name of the specific implementing class used by default.

### Mermaid Code

```mermaid
sequenceDiagram
    actor Client

    %% Define styles for classes and interfaces
    classDef interface fill:#E8F3FD,stroke:#3471A5,stroke-width:2px
    classDef class fill:#FFF2CC,stroke:#D6B656,stroke-width:2px

    %% Participant Definitions
    participant FCP as FilterChainProxy
    participant ETF as ExceptionTranslationFilter
    participant AEP as "<<interface>> <br> AuthenticationEntryPoint"
    participant UAPF as UsernamePasswordAuthenticationFilter
    participant AM as "<<interface>> <br> AuthenticationManager"
    participant DAO as DaoAuthenticationProvider
    participant UDS as "<<interface>> <br> UserDetailsService"
    participant PE as "<<interface>> <br> PasswordEncoder"
    participant SH as "<<interface>> <br> AuthenticationSuccessHandler"
    participant SCH as SecurityContextHolder
    participant SCR as "<<interface>> <br> SecurityContextRepository"

    %% Apply styles
    class AEP,AM,UDS,PE,SH,SCR interface
    class FCP,ETF,UAPF,DAO,SCH class

    %% --- Flow 1: Unauthenticated Request & Redirect ---

    box rgb(240, 240, 240)
    title Flow 1: Unauthenticated Request & Redirect
    Client->>FCP: 1. GET /protected-resource
    activate FCP
    FCP->>ETF: 2. doFilter (catches Security Exceptions)
    activate ETF
    note right of ETF: AuthorizationFilter (not shown) throws AccessDeniedException
    ETF->>AEP: 3. commence()
    activate AEP
    note right of AEP: Implementation is LoginUrlAuthenticationEntryPoint
    AEP->>Client: 4. HTTP 302 Redirect to /login
    deactivate AEP
    deactivate ETF
    deactivate FCP
    end

    %% --- Flow 2: Login Submission & Authentication ---

    box rgb(230, 245, 230)
    title Flow 2: Login Submission & Authentication
    Client->>FCP: 5. POST /login (username, password)
    activate FCP
    FCP->>UAPF: 6. doFilter
    activate UAPF
    UAPF->>UAPF: 7. Creates UsernamePasswordAuthenticationToken (unauthenticated)
    UAPF->>AM: 8. authenticate(token)
    activate AM
    note right of AM: Implementation is ProviderManager
    AM->>DAO: 9. authenticate(token)
    activate DAO
    DAO->>UDS: 10. loadUserByUsername(username)
    activate UDS
    UDS-->>DAO: 11. returns UserDetails
    deactivate UDS
    DAO->>PE: 12. matches(rawPassword, storedPassword)
    activate PE
    PE-->>DAO: 13. returns true
    deactivate PE
    DAO-->>AM: 14. returns Authentication (authenticated)
    deactivate DAO
    AM-->>UAPF: 15. returns Authentication
    deactivate AM

    UAPF->>SCH: 16. setContext(newlyAuthenticated)

    UAPF->>SCR: 17. saveContext()
    activate SCR
    note right of SCR: Implementation is HttpSessionSecurityContextRepository
    SCR-->>UAPF:
    deactivate SCR

    UAPF->>SH: 18. onAuthenticationSuccess()
    activate SH
    note right of SH: Implementation is SavedRequestAwareAuthenticationSuccessHandler
    SH->>Client: 19. HTTP 302 Redirect to /protected-resource
    deactivate SH
    deactivate UAPF
    deactivate FCP
    end
```

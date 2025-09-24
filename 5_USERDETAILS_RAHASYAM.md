# Chapter 5: The UserDetails Secret (UserDetails Rahasyam 🤫)

`DaoAuthenticationProvider` ki user details kaavali. Kani aa details ekkada nunchi vastayi? Database nuncha? In-memory nuncha? LDAP nuncha? Ee provider ki ee vishayam anavasaram. Daaniki kavalsindalla okate: `UserDetailsService` ane oka "secret agent" nunchi `UserDetails` object teeskuni ravali.

Let's look at these two important interfaces.

## Interface: `UserDetails`

Idi modati rahasyam. Idi oka particular user gurinchi information hold chestundi. Deenilo unna important methods:
-   `getUsername()`
-   `getPassword()` (Important: idi encoded password avvali!)
-   `getAuthorities()` (Roles/Permissions)
-   `isAccountNonExpired()`, `isAccountNonLocked()`, etc.

## Interface: `UserDetailsService`

Idi rendo rahasyam, and the most important one. Idi oka interface. Deenilo oke okka method untundi: `loadUserByUsername(String username)`. Ee method username teeskuni, venakki oka `UserDetails` object ni istundi.

**Idi oka bridge anamata**. `DaoAuthenticationProvider` ki mariyu mana user data (database, etc.) ki madhya lo ee bridge untundi.

Now, let's see the different implementations of this interface in our project.

---

### Implementation 1: `InMemoryUserDetailsManager` (The Default World 🏞️)

Idi Spring Security tho vache oka concrete **class**. Idi `UserDetailsService` interface ni implement chestundi.

**Use Case:** Simple applications, testing, or demos kosam, manam users ni database lo kakunda, direct ga code lo ne define cheyyali anukunnappudu idi vaadatham.

Mana **`default`** profile lo, manam `DefaultSecurityConfig.java` lo ee class ni use chesi, `userDetailsService` ane bean ni create chesam.

`src/main/java/com/example/springsecuritystory/config/DefaultSecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@Profile("default")
public class DefaultSecurityConfig {

    // ... other beans

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();

        UserDetails admin = User.builder()
            .username("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("ADMIN", "USER")
            .build();

        // Ikkada chudandi: manam InMemoryUserDetailsManager ni return chestunnam
        return new InMemoryUserDetailsManager(user, admin);
    }

    // ...
}
```

---

### Implementation 2: `CustomUserDetailsService` (Our Custom World 🧑‍💻)

Real-world applications lo, users database lo untaru. So, manam `UserDetailsService` interface ni mana sonta class tho implement cheyyali.

Mana **`custom`** profile lo, manam `CustomUserDetailsService` ane class create chesam.

`src/main/java/com/example/springsecuritystory/service/CustomUserDetailsService.java`
```java
@Service
@Profile("custom")
public class CustomUserDetailsService implements UserDetailsService {

    // ... (simulating a user list)

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Our logic to get user from a list (or a database)
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("User not found..."));
    }
}
```
Ikkada manam `UserDetailsService` ni implement chesi, mana custom logic raastunnam. `@Profile("custom")` annotation tho, ee class `custom` profile active ga unnapudu matrame create avtundi.

Spring automatic ga ee implementation ni `DaoAuthenticationProvider` ki supply chestundi. Anduke deenini "secret agent" annam!

**Next Stop:** Let's put it all together and look at our custom world.

[<-- Previous Chapter](./4_PROVIDER_AND_DAO_MAAYALOKAM.md) | [<-- Back to Main Story](./SPRING_SECURITY_KATHA.md) | [Next Chapter -->](./6_CUSTOM_LOKAM.md)

# Chapter 6: Our Custom World (Mee Sonta Prapancham 🧑‍💻)

Eppudu manam katha antha chusam. Default ga Spring ela pani chestundo, and manam daanini ela customize cheyyagalamo chusam. Let's review the two worlds we built.

## The Two Worlds: Default vs. Custom

Mana project lo ippudu rendu prapanchalu unnayi, and manam Spring Profiles tho vaati madhya switch avvochu. Ee rendu prapanchalaki unna theda vaati Security Configuration lone undi.

---

### Default World Configuration (`@Profile("default")`)

Idi `DefaultSecurityConfig.java` lo undi. Deeni key feature entante, `UserDetailsService` bean ni **ikkade, local ga** define chestundi, `InMemoryUserDetailsManager` ni use chesi.

`src/main/java/com/example/springsecuritystory/config/DefaultSecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@Profile("default")
public class DefaultSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/home").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails user = User.builder()
            .username("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

### Custom World Configuration (`@Profile("custom")`)

Idi `CustomSecurityConfig.java` lo undi. Ikkada manam `UserDetailsService` bean ni define cheyyatledu. Manam create chesina `CustomUserDetailsService` (`@Service` and `@Profile("custom")` tho) ni Spring automatic ga pick cheskuntundani nammutunnam.

`src/main/java/com/example/springsecuritystory/config/CustomSecurityConfig.java`
```java
@Configuration
@EnableWebSecurity
@Profile("custom")
public class CustomSecurityConfig {

    // Notice: No UserDetailsService bean here!

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/home").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```
Ee approach tho, manam a profile ni activate chestamo, daaniki sambandinchina security rules matrame apply avtayi.

## The Final Picture

Mana `customuser` login avvali ante, journey ila untundi:

`Filter` -> `Manager` -> `Provider` -> `CustomUserDetailsService` -> `PasswordEncoder` -> **SUCCESS!** 🎉

Ippudu "Nenu, the Request" successfully authenticate ayyi, `SecurityContext` lo save avtundi. Finally, mana destination aina `/welcome` page ki reach avtundi.

**The End.**

Congratulations! You have completed the story of Spring Security Username/Password Authentication.

[<-- Previous Chapter](./5_USERDETAILS_RAHASYAM.md) | [<-- Back to Main Story](./SPRING_SECURITY_KATHA.md)

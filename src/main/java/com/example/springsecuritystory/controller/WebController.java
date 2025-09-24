package com.example.springsecuritystory.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping({"/", "/home"})
    public String home() {
        return "home"; // Returns home.html
    }

    @GetMapping("/welcome")
    public String welcome() {
        return "welcome"; // Returns welcome.html (secured)
    }

    // Spring Security provides a default /login endpoint, but if you want a custom one,
    // you can map it here. For now, we'll use the default.
    // @GetMapping("/login")
    // public String login() {
    //     return "login"; // Returns login.html
    // }
}

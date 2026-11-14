package com.hiddengems.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String home() {
        return "forward:/landing.html";
    }

    @GetMapping("/explore")
    public String explore() {
        return "forward:/home.html";
    }

    @GetMapping("/index.html")
    public String redirectIndexToRoot() {
        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login-page.html";
    }

    @GetMapping("/login.html")
    public String redirectLoginToCleanUrl() {
        return "redirect:/login";
    }
}

package com.isaac.approvalworkflowengine.shared.web;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Lightweight routes for platform entry points and browser icon probes.
 */
@Controller
public class PlatformEntryController {

    @GetMapping("/")
    public String root() {
        return "redirect:/swagger-ui/index.html";
    }

    @GetMapping({"/favicon.ico", "/apple-touch-icon.png", "/apple-touch-icon-precomposed.png"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void browserIconProbes() {
    }
}

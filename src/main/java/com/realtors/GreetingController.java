package com.realtors;

import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/greeting")
public class GreetingController {

    @GetMapping
    public Map<String, String> getGreeting() {
        return Map.of("message", "This is Diamond Reality Services Application");
    }
}

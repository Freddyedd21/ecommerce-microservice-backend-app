package com.selimhorri.app.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RootController {

    @GetMapping({ "", "/" })
    ResponseEntity<String> root() {
        // Provide a simple OK response so health checks and manual probes do not
        // produce 404 traces.
        return ResponseEntity.ok("cloud-config-service");
    }
}

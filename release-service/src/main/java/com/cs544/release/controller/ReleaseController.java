package com.cs544.release.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cs544.release.model.Release;
import com.cs544.release.repository.ReleaseRepository;
import com.cs544.release.security.JwtUtil;
import com.cs544.release.service.ReleaseEventProducer;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {
    private final ReleaseRepository releaseRepository;
    private final ReleaseEventProducer eventProducer;
    private final JwtUtil jwtUtil;

    public ReleaseController(ReleaseRepository releaseRepository, ReleaseEventProducer eventProducer, JwtUtil jwtUtil) {
        this.releaseRepository = releaseRepository;
        this.eventProducer = eventProducer;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping
    public ResponseEntity<Release> createRelease(@RequestBody ReleaseRequest request) {
        Release release = releaseRepository.save(new Release(request.name(), request.version()));
        eventProducer.publishReleaseCreated(release);
        return ResponseEntity.ok(release);
    }

    @GetMapping
    public ResponseEntity<List<Release>> listReleases() {
        return ResponseEntity.ok(releaseRepository.findAll());
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> token(@RequestBody TokenRequest request) {
        return ResponseEntity.ok(new TokenResponse(jwtUtil.generateToken(request.username())));
    }

    public record ReleaseRequest(String name, String version) {
    }

    public record TokenRequest(String username) {
    }

    public record TokenResponse(String token) {
    }
}

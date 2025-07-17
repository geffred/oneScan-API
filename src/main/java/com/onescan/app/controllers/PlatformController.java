package com.onescan.app.controllers;

import com.onescan.app.DTO.PlatformDTO;
import com.onescan.app.services.PlatformService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/platforms")
@RequiredArgsConstructor
public class PlatformController {

    private final PlatformService platformService;

    @PostMapping
    public ResponseEntity<PlatformDTO> create(@RequestBody PlatformDTO dto) {
        return ResponseEntity.ok(platformService.createPlatform(dto));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<PlatformDTO>> getUserPlatforms(@PathVariable Long userId) {
        return ResponseEntity.ok(platformService.getUserPlatforms(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlatformDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(platformService.getPlatformById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlatformDTO> update(@PathVariable Long id, @RequestBody PlatformDTO dto) {
        return ResponseEntity.ok(platformService.updatePlatform(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        platformService.deletePlatform(id);
        return ResponseEntity.noContent().build();
    }

}
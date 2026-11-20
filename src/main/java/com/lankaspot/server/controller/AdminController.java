package com.lankaspot.server.controller;

import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.admin.AdminListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    private final AdminListingService adminListingService;

    @GetMapping("/send-email")
    public ResponseEntity<LankaspotResponse<Void>> sendEmail() {
        return ResponseEntity.ok(adminListingService.sendTestEmail());
    }

    @PostMapping("/cache-invalidate-advertisement")
    public ResponseEntity<LankaspotResponse<UUID>> invalidateAdvertisementCaches(@RequestParam UUID advertisementRef, @RequestParam(defaultValue = "true") boolean refresh) {
        return ResponseEntity.ok(adminListingService.invalidateAdvertisementCaches(advertisementRef, refresh));
    }

    @PostMapping("/cache-advertisement")
    public ResponseEntity<LankaspotResponse<UUID>> cacheAdvertisement(@RequestParam UUID advertisementRef){
        return ResponseEntity.ok(adminListingService.cacheAdvertisement(advertisementRef));
    }

}
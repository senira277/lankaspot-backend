package com.lankaspot.server.controller;

import com.lankaspot.server.dto.openRetrievals.*;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.OpenRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/open-retrievals")
@RequiredArgsConstructor
@Slf4j
public class OpenRetrievalsController {
    private final OpenRetrievalService openRetrievalService;

    @GetMapping("/get-all-categories")
    public ResponseEntity<LankaspotResponse<AllCategories>> getAllCategories() {
        log.info("Fetching all categories");
        return ResponseEntity.ok(openRetrievalService.getAllCategories());
    }

    @GetMapping("/get-all-districts")
    public ResponseEntity<LankaspotResponse<AllDistricts>> getAllDistricts() {
        log.info("Fetching all districts");
        return ResponseEntity.ok(openRetrievalService.getAllDistricts());
    }

    @GetMapping("/get-all-cities")
    public ResponseEntity<LankaspotResponse<AllCities>> getAllCities(@RequestParam Long districtId) {
        log.info("Fetching all cities for districtId: {}", districtId);
        return ResponseEntity.ok(openRetrievalService.getAllCities(districtId));
    }

    @GetMapping("/get-all-features")
    public ResponseEntity<LankaspotResponse<AllFeatures>> getAllFeatures(
            @RequestParam Long categoryId) {
        log.info("Fetching all features for categoryId: {}", categoryId);
        return ResponseEntity.ok(
                openRetrievalService.getAllFeatures(categoryId)
        );
    }

    @GetMapping("/get-all-functional-groups")
    public ResponseEntity<LankaspotResponse<AllFunctionalGroups>> getAllFunctionalGroups() {
        log.info("Fetching all functional groups");
        return ResponseEntity.ok(openRetrievalService.getAllFunctionalGroups());
    }
}
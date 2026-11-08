package com.lankaspot.server.controller;

import com.lankaspot.server.dto.reviewer.ReviewConfirmationRequest;
import com.lankaspot.server.dto.reviewer.ReviewerConfirmationResponse;
import com.lankaspot.server.dto.reviewer.retrieve.*;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.reviewer.ReviewerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/reviewer")
@PreAuthorize("hasRole('REVIEWER')")
public class ReviewerController {
    private final ReviewerService reviewerService;

    @GetMapping("/get-review-queue")
    public ResponseEntity<LankaspotResponse<ReviewerQueueDTO>> getReviewQueue(@RequestParam int page, @RequestParam int size) {
        return ResponseEntity.ok(reviewerService.getReviewerQueue(page, size));
    }

    @GetMapping("/get-advertisement-for-review")
    public ResponseEntity<LankaspotResponse<GetReviewerFullAdvertisement>> getAdvertisementForReview(@RequestParam String advertisementRef) {
        return ResponseEntity.ok(reviewerService.getAdvertisementForReview(advertisementRef));
    }

    @PostMapping("/reviewer-confirmation")
    public ResponseEntity<LankaspotResponse<ReviewerConfirmationResponse>> reviewerConfirmation(@RequestBody ReviewConfirmationRequest request, Authentication authentication) {
        return ResponseEntity.ok(reviewerService.reviewerConfirmation(request, authentication));
    }

    @GetMapping("/get-review-resubmit-queue")
    public ResponseEntity<LankaspotResponse<GetReviewerResubmitQueueDTO>> getReviewResubmitQueue(@RequestParam int page, @RequestParam int size) {
        return ResponseEntity.ok(reviewerService.getReviewerResubmitQueue(page, size));
    }
}

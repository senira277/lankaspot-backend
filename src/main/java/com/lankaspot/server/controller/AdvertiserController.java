package com.lankaspot.server.controller;

import com.lankaspot.server.dto.advertiser.advertisement.AdvertisementDTO;
import com.lankaspot.server.dto.advertiser.advertisement.GetAdvertisementDTOWithReviewerComment;
import com.lankaspot.server.dto.advertiser.advertisement.GetAllAdvertisementSummaryResponse;
import com.lankaspot.server.dto.advertiser.discount.ValidateDiscountCodeRequest;
import com.lankaspot.server.dto.advertiser.discount.ValidateDiscountCodeResponseData;
import com.lankaspot.server.dto.advertiser.payment.PayhereCheckoutRequest;
import com.lankaspot.server.dto.advertiser.payment.PayhereCheckoutResponseData;
import com.lankaspot.server.dto.advertiser.retrieve.GetCurrentAdvertiserContact;
import com.lankaspot.server.dto.advertiser.retrieve.GetQuoteWithMediaResponseData;
import com.lankaspot.server.dto.advertiser.retrieve.GetResumeQuoteResponseData;
import com.lankaspot.server.dto.advertiser.review.GetReviewQuoteByUserResponseData;
import com.lankaspot.server.dto.advertiser.review.ReviewQuoteByUserRequestDTO;
import com.lankaspot.server.dto.advertiser.savePropertyInfo.SavePropertyInfoRequest;
import com.lankaspot.server.dto.advertiser.savePropertyInfo.SaveAdvertPropertyInfoResponseData;
import com.lankaspot.server.dto.advertiser.subscription.*;
import com.lankaspot.server.dto.advertiser.uploads.RemoveAdvertMediaResponseData;
import com.lankaspot.server.dto.advertiser.uploads.SetPrimaryAdvertMediaRequest;
import com.lankaspot.server.dto.advertiser.uploads.SetPrimaryAdvertMediaResponseData;
import com.lankaspot.server.dto.advertiser.uploads.UploadAdvertMediaResponseData;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.AdvertiserService;
import com.lankaspot.server.service.payment.PayhereCheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/advertiser")
@PreAuthorize("hasRole('ADVERTISER')")
@RequiredArgsConstructor
@Slf4j
public class AdvertiserController {
    private final AdvertiserService advertiserService;
    private final PayhereCheckoutService payhereCheckoutService;

    @PostMapping("/save-property-info")
    public ResponseEntity<LankaspotResponse<SaveAdvertPropertyInfoResponseData>> savePropertyInfo(@Valid @RequestBody SavePropertyInfoRequest request, Authentication authentication) {
        log.info("Saving property info for advertiser: {}", authentication.getName());
        return ResponseEntity.ok(advertiserService.savePropertyInfo(request, authentication));
    }

    @PostMapping(value = "/upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LankaspotResponse<UploadAdvertMediaResponseData>> uploadMedia(@RequestParam("quoteRef") Long quoteRef, @RequestParam("primaryIndex") Integer primaryIndex, @RequestPart("photos") List<MultipartFile> photos, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.uploadMedia(
                        quoteRef, primaryIndex, photos, authentication
                )
        );
    }

    @DeleteMapping("/remove-media")
    public ResponseEntity<LankaspotResponse<RemoveAdvertMediaResponseData>> removeMedia(@RequestParam("quoteRef") Long quoteRef, @RequestParam("mediaId") Long mediaId, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.requestMediaRemoval(
                        quoteRef, mediaId, authentication
                )
        );
    }

    @PatchMapping("/media/change-primary")
    public ResponseEntity<LankaspotResponse<SetPrimaryAdvertMediaResponseData>>
    setPrimaryMedia(@Valid @RequestBody SetPrimaryAdvertMediaRequest request, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.setPrimaryMedia(request, authentication)
        );
    }

    @GetMapping("/get-quote-with-media")
    public ResponseEntity<LankaspotResponse<GetQuoteWithMediaResponseData>> getQuote(@RequestParam("quoteRef") Long quoteRef, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.getQuote(quoteRef, authentication)
        );
    }

    @GetMapping("/get-current-advertiser-contacts")
    public LankaspotResponse<GetCurrentAdvertiserContact> getContacts(Authentication authentication) {
        return advertiserService.getContacts(authentication);
    }

    @GetMapping("/get-resume-quote")
    public LankaspotResponse<GetResumeQuoteResponseData> getResumeQuote(Authentication authentication) {
        return advertiserService.getResumeQuote(authentication);
    }

    @PostMapping("/review-quote")
    public ResponseEntity<LankaspotResponse<GetReviewQuoteByUserResponseData>> reviewQuote(@RequestBody ReviewQuoteByUserRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.reviewQuoteByUser(request, authentication)
        );
    }

    @GetMapping("/get-subscription-plans")
    public ResponseEntity<LankaspotResponse<GetSubscriptionPlansResponseData>> getSubscriptionPlans() {
        return ResponseEntity.ok(advertiserService.getSubscriptionPlans());
    }

    @GetMapping("/get-advertisement-tiers")
    public ResponseEntity<LankaspotResponse<GetAdvertisementTiersResponseData>> getAdvertisementTiers() {
        return ResponseEntity.ok(advertiserService.getAdvertisementTiers());
    }

    @GetMapping("/get-quote-plan-selection")
    public ResponseEntity<LankaspotResponse<GetAdvertQuotePlanSelectionResponseData>> getQuotePlanSelection(@RequestParam Long quoteRef, Authentication authentication) {
        return ResponseEntity.ok(advertiserService.getQuotePlanSelection(quoteRef, authentication));
    }

    @PostMapping("/plan")
    public ResponseEntity<LankaspotResponse<SelectAdvertPlanResponseData>> selectPlan(@Valid @RequestBody SelectAdvertPlanRequest request, Authentication authentication) {
        return ResponseEntity.ok(advertiserService.selectPlan(request, authentication));
    }

    @PostMapping("/validate-discount-code")
    public ResponseEntity<LankaspotResponse<ValidateDiscountCodeResponseData>> validateDiscountCode(@Valid @RequestBody ValidateDiscountCodeRequest request, Authentication authentication) {
        return ResponseEntity.ok(advertiserService.validateDiscountCode(request, authentication));
    }

    @PostMapping("/register-payment")
    public ResponseEntity<LankaspotResponse<PayhereCheckoutResponseData>> registerPayment(@Valid @RequestBody PayhereCheckoutRequest request, Authentication authentication) {
        return ResponseEntity.ok(payhereCheckoutService.startCheckout(request, authentication));
    }

    @GetMapping("/check-payment-status/{orderId}")
    public ResponseEntity<LankaspotResponse<Boolean>> checkPaymentStatus(@PathVariable String orderId, Authentication authentication) {
        log.info("Checking payment status for orderId: {} by user: {}", orderId, authentication.getName());
        return ResponseEntity.ok(advertiserService.checkPaymentStatus(orderId,authentication));
    }

    @GetMapping("/get-all-advertisements")
    public ResponseEntity<LankaspotResponse<GetAllAdvertisementSummaryResponse>> getAllAdvertisements(@RequestParam int page, @RequestParam int size, Authentication authentication) {
        log.info("Fetching all advertisements for user: {}", authentication.getName());
        return ResponseEntity.ok(advertiserService.getAllAdvertisements(authentication, page, size));
    }

    @GetMapping("/get-advertisement")
    public ResponseEntity<LankaspotResponse<GetAdvertisementDTOWithReviewerComment>> getAdvertisement(@RequestParam String advertisementRef, Authentication authentication) {
        log.info("Fetching advertisement details for advertisementRef: {} by user: {}", advertisementRef, authentication.getName());
        return ResponseEntity.ok(advertiserService.getAdvertisementByRef(advertisementRef, authentication));
    }

    @PostMapping("/amend-property-info")
    public ResponseEntity<LankaspotResponse<SaveAdvertPropertyInfoResponseData>> amendPropertyInfo(@Valid @RequestBody SavePropertyInfoRequest request, Authentication authentication) {
        log.info("Amending property info for advertiser: {}", authentication.getName());
        return ResponseEntity.ok(advertiserService.amendPropertyInfo(request, authentication));
    }

    @PostMapping(value = "/amend-upload-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<LankaspotResponse<UploadAdvertMediaResponseData>> amendUploadMedia(@RequestParam("quoteRef") Long quoteRef, @RequestParam("primaryIndex") Integer primaryIndex, @RequestPart("photos") List<MultipartFile> photos, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.amendUploadMedia(
                        quoteRef, primaryIndex, photos, authentication
                )
        );
    }

    @PostMapping("/amend-review-quote")
    public ResponseEntity<LankaspotResponse<GetReviewQuoteByUserResponseData>> amendReviewQuote(@RequestBody ReviewQuoteByUserRequestDTO request, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.amendReviewQuoteByUser(request, authentication)
        );
    }

    @DeleteMapping("/amend-remove-media")
    public ResponseEntity<LankaspotResponse<RemoveAdvertMediaResponseData>> amendRemoveMedia(@RequestParam("quoteRef") Long quoteRef, @RequestParam("mediaId") Long mediaId, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.amendRequestMediaRemoval(
                        quoteRef, mediaId, authentication
                )
        );
    }

    @PatchMapping("/media/amend-change-primary")
    public ResponseEntity<LankaspotResponse<SetPrimaryAdvertMediaResponseData>> amendSetPrimaryMedia(@Valid @RequestBody SetPrimaryAdvertMediaRequest request, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.amendSetPrimaryMedia(request, authentication)
        );
    }

    @PatchMapping("/media/amend-set-all-media-primary-false")
    public ResponseEntity<LankaspotResponse<?>> amendSetAllMediaPrimaryFalse(@Valid @RequestBody Long quoteRef, Authentication authentication) {
        return ResponseEntity.ok(
                advertiserService.amendSetAllMediaPrimaryFalse(quoteRef, authentication)
        );
    }



}

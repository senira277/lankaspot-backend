package com.lankaspot.server.controller;

import com.lankaspot.server.dto.listings.GetAllListingsResponse;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.ListingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/listings")
@RequiredArgsConstructor
public class ListingsController {
    private final ListingsService listingsService;

    @GetMapping("/getAllListings")
    public LankaspotResponse<GetAllListingsResponse> getAllListings(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long districtId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return LankaspotResponse.success(listingsService.getAllListings(
                categoryId, cityId, districtId, minPrice, maxPrice, page, size));
    }
}

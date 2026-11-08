package com.lankaspot.server.service.payment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.lankaspot.server.config.PayhereProperties;
import com.lankaspot.server.dto.advertiser.payment.PayhereCheckoutRequest;
import com.lankaspot.server.dto.advertiser.payment.PayhereCheckoutResponseData;
import com.lankaspot.server.enums.*;
import com.lankaspot.server.exception.*;
import com.lankaspot.server.model.*;
import com.lankaspot.server.repository.*;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.DiscountCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class PayhereCheckoutService {

    private static final Set<AdQuoteStatus> EDITABLE_STATUSES =
            EnumSet.of(AdQuoteStatus.IN_COMPLETE, AdQuoteStatus.COMPLETE);
    private static final String ORDER_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PayhereProperties props;
    private final PayhereHashService hashService;
    private final ObjectMapper objectMapper;
    private final AdQuoteRepository adQuoteRepository;
    private final AdQuoteSubscriptionPlanRepository adQuoteSubscriptionPlanRepository;
    private final AdQuoteTierRepository adQuoteTierRepository;
    private final ContactRepository contactRepository;
    private final DiscountCodeService discountCodeService;
    private final PaymentRegistryRepository registryRepository;
    private final EntityMappingRepository entityMappingRepository;
    private final PayHerePaymentNotificationRepository payHerePaymentNotificationRepository;
    private final SupportContactRepository supportContactRepository;

    @Transactional
    public LankaspotResponse<PayhereCheckoutResponseData> startCheckout(PayhereCheckoutRequest request, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());
        Long quoteRef = request.quoteRef();
        List<SupportContact> supportContacts = supportContactRepository.findActiveSupportContacts();
        if(checkPaymentRegistryExistsForQuote(quoteRef, authentication)) {
            throw new PaymentAlreadyCompletedException("You have Already Purchased This Quote. Please contact " + supportContacts.get(0).getPhoneNumber() + " if you have any issues.");
        } else if (checkPolicyAlreadyCreated(quoteRef, authentication)) {
            throw new PaymentAlreadyCompletedException("You have Already Created a Policy for This Quote. Please contact " + supportContacts.get(0).getPhoneNumber() + " if you have any issues.");
        }

        if (quoteRef == null) {
            throw new IllegalArgumentException("quoteRef is required");
        }

        // Row lock: serialises concurrent checkouts and plan changes on the same quote.
        AdQuote quote = adQuoteRepository.findByIdForUpdate(quoteRef)
                .filter(q -> Objects.equals(q.getAdvertiserId(), advertiserId))
                .orElseThrow(() -> new QuoteNotFoundException(quoteRef));

        if (!EDITABLE_STATUSES.contains(quote.getStatus())) {
            throw new QuoteNotEditableException(quoteRef, String.valueOf(quote.getStatus()));
        }
        if (quote.getLastCompletedStep() < 4) {
            throw new IllegalArgumentException("Complete the plan selection before checkout.");
        }

        // Plan + add-ons come from the DB, never from the client.
        SubscriptionPlan plan = adQuoteSubscriptionPlanRepository.findByAdQuote_Id(quoteRef)
                .map(AdQuoteSubscriptionPlan::getSubscriptionPlan)
                .orElseThrow(() -> new IllegalArgumentException("No plan selected for this quote."));

        if (plan.getStatus() != SubscriptionPlanStatus.ACTIVE) {
            throw new PlanNotAvailableException(plan.getId());
        }

        List<AdvertisementTier> tiers = adQuoteTierRepository.findByAdQuote_Id(quoteRef).stream()
                .map(AdQuoteTier::getAdvertisementTier)
                .sorted(Comparator.comparing(AdvertisementTier::getId))
                .toList();

        for (AdvertisementTier tier : tiers) {
            if (!tier.isActive()) throw new TierNotAvailableException(tier.getId());
        }

        // Pricing snapshot.
        List<Map<String, Object>> lines = new ArrayList<>();
        lines.add(line("PLAN", plan.getId(), plan.getName(), plan.getPrice(),
                "Listing visible for " + switch (plan.getBillingPeriod()) {
                    case MONTHLY -> "1 month";
                    case SIX_MONTHS -> "6 months";
                    case YEARLY -> "1 year";
                }));
        tiers.forEach(t -> lines.add(line("ADD_ON", t.getId(), t.getName(), t.getPrice(),
                "Runs for " + (t.getDurationDays() == 1 ? "1 day" : t.getDurationDays() + " days"))));
        BigDecimal subtotal = lines.stream()
                .map(l -> (BigDecimal) l.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Validated here, consumed only when the notify confirms payment.
        Long discountCodeId = null;
        String discountCode = null;
        BigDecimal discountPct = null;
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2);

        String code = request.discountCode();
        if (code != null && !code.isBlank()) {
            DiscountCode discount = discountCodeService.findApplicableDiscount(code.trim(), authentication.getName());
            discountCodeId = discount.getId();
            discountCode = discount.getCode();
            discountPct = discount.getDiscountPercentage();
            discountAmount = subtotal.multiply(discountPct)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal finalAmount = subtotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        if (finalAmount.signum() <= 0) {
            throw new IllegalArgumentException("This order has no payable amount.");
        }

        String orderId = generateOrderId(quoteRef);
        String currency = plan.getCurrency();
        String formattedAmount = PayhereHashService.formatAmount(finalAmount);

        Customer customer = resolveCustomer(authentication);
        String hash = hashService.checkoutHash(orderId, formattedAmount, currency);

        PaymentRegistry registry = new PaymentRegistry();
        registry.setOrderId(orderId);
        registry.setQuoteRef(quoteRef);
        registry.setPayerId(advertiserId);
        registry.setPlanId(plan.getId());
        registry.setLineItemsSnapshot(toJson(lines));
        registry.setSubtotal(subtotal);
        registry.setBillingName((customer.firstName() + " " + customer.lastName()).trim());
        registry.setBillingEmail(customer.email());
        registry.setListingTitle(quote.getTitle());
        registry.setDiscountCodeId(discountCodeId);
        registry.setDiscountCode(discountCode);
        registry.setDiscountPercentage(discountPct);
        registry.setDiscountAmount(discountAmount);
        registry.setFinalAmount(finalAmount);
        registry.setCurrency(currency);
        registry.setStatus(PaymentRegistryStatus.PENDING);
        registry.setHashCode(hash);

        log.info("PayHere checkout hash generated: orderId={}, hash={}", orderId, hash);
        registryRepository.save(registry);

        log.info("PayHere checkout registered: orderId={}, quoteRef={}, amount={} {}",
                orderId, quoteRef, formattedAmount, currency);

        String cancelUrl = props.cancelBaseUrl() + "?quoteRef=" + quoteRef + "&step=" + 6;
        String returnUrl = props.returnBaseUrl() + "?quoteRef=" + quoteRef + "&step=" + 6;

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", props.merchantId());
        fields.put("notify_url", props.notifyUrl());
        fields.put("order_id", orderId);
        fields.put("items", itemsDescription(plan, tiers));
        fields.put("currency", currency);
        fields.put("amount", formattedAmount);
        fields.put("first_name", customer.firstName());
        fields.put("last_name", customer.lastName());
        fields.put("return_url", returnUrl);
        fields.put("cancel_url", cancelUrl);
        fields.put("email", customer.email());
        fields.put("phone", customer.phone());
        fields.put("address", "N/A");
        fields.put("city", "Colombo");
        fields.put("country", "Sri Lanka");
        fields.put("hash", hash);

        return LankaspotResponse.success(
                new PayhereCheckoutResponseData(orderId, props.actionUrl(), fields),
                "Checkout created successfully");
    }

    @Transactional(readOnly = true)
    public Boolean checkPaymentRegistryExistsForQuote(Long quoteRef, Authentication authentication) {
        try{
            // check 1. If the quote exists and belongs to the authenticated user
            AdQuote quote = adQuoteRepository.findById(quoteRef)
                    .filter(q -> Objects.equals(q.getAdvertiserId(), UUID.fromString(authentication.getName())))
                    .orElseThrow(() -> new QuoteNotFoundException(quoteRef));
            //check 2. If quote has a entry in the payment registry with status SUCCESS
            return registryRepository.existsByQuoteRefAndPayerIdAndStatus(quoteRef, UUID.fromString(authentication.getName()), PaymentRegistryStatus.SUCCESS);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Transactional(readOnly = true)
    public Boolean checkPolicyAlreadyCreated(Long quoteRef, Authentication authentication) {
        try{
            // check 1. If the quote exists and belongs to the authenticated user
            AdQuote quote = adQuoteRepository.findById(quoteRef)
                    .filter(q -> Objects.equals(q.getAdvertiserId(), UUID.fromString(authentication.getName())))
                    .orElseThrow(() -> new QuoteNotFoundException(quoteRef));
            //check 2. If quote has a policy linked in the entity mapping
            return entityMappingRepository.existsByQuote_IdAndMappingType(quoteRef,EntityMappingType.QUOTE_POLICY);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /* ------------------------------ helpers ------------------------------ */

    private record Customer(String firstName, String lastName, String email, String phone) {}

    /**
     * ASSUMPTION: the principal is a Keycloak JWT with given_name / family_name / email claims.
     * The phone is the first VERIFIED contact, the same source getContacts uses.
     */
    private Customer resolveCustomer(Authentication authentication) {
        String first = null, last = null, email = null;
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            first = jwt.getClaimAsString("given_name");
            last = jwt.getClaimAsString("family_name");
            email = jwt.getClaimAsString("email");
        }

        String phone = contactRepository
                .findByUser_KeycloakUserIdAndStatus(authentication.getName(), ContactStatus.VERIFIED)
                .stream().map(Contact::getPhoneNumber).findFirst().orElse(null);

        return new Customer(
                orDefault(first, "Customer"),
                orDefault(last, "-"),
                orDefault(email, "noreply@lankaspot.lk"),
                orDefault(phone, "0000000000"));
    }

    private static String generateOrderId(Long quoteRef) {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            suffix.append(ORDER_ID_ALPHABET.charAt(RANDOM.nextInt(ORDER_ID_ALPHABET.length())));
        }
        return "LS-" + quoteRef + "-" + suffix;
    }

    private static Map<String, Object> line(String kind, Long id, String name, BigDecimal amount, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("kind", kind);
        m.put("id", id);
        m.put("name", name);
        m.put("detail", detail);
        m.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        return m;
    }

    private static String itemsDescription(SubscriptionPlan plan, List<AdvertisementTier> tiers) {
        String text = "LankaSpot ad: " + plan.getName();
        if (!tiers.isEmpty()) text += " + " + tiers.size() + " add-on" + (tiers.size() > 1 ? "s" : "");
        return text.length() > 100 ? text.substring(0, 100) : text;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Could not serialize line items", e);
        }
    }

    private static String orDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
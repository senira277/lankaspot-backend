package com.lankaspot.server.dto.advertiser.invoice;

import java.math.BigDecimal;

public record InvoiceLineResponse(
        String kind, String name, String detail, BigDecimal amount
) {}


package com.lankaspot.server.dto.advertiser.retrieve;

import com.lankaspot.server.dto.common.ContactDTO;

import java.util.List;

public record GetCurrentAdvertiserContact(
        List<ContactDTO> phoneNumbers
) {
}

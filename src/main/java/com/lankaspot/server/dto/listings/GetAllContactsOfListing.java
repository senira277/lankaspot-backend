package com.lankaspot.server.dto.listings;

import com.lankaspot.server.dto.common.ContactDTO;

import java.util.List;

public record GetAllContactsOfListing(
        List<ContactDTO> contacts
) {
}

package com.lankaspot.server.constants;

import com.lankaspot.server.enums.Role;

import java.util.List;

public final class RoleConstants {

    private RoleConstants() {
    }

    public static final List<Role> ROLE_PRIORITY = List.of(
            Role.SUPER_ADMIN,
            Role.ADMIN,
            Role.REVIEWER,
            Role.ADVERTISER,
            Role.USER
    );
}
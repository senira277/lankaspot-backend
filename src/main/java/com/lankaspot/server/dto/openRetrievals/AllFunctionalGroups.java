package com.lankaspot.server.dto.openRetrievals;

import com.lankaspot.server.model.FunctionalGroup;

import java.util.List;

public record AllFunctionalGroups(
        List<FunctionalGroup> functionalGroups
) {
}

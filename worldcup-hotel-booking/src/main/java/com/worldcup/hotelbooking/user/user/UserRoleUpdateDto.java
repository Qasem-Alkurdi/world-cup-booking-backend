package com.worldcup.hotelbooking.user.user;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UserRoleUpdateDto(
        @NotNull Set<Role> roles
) {}
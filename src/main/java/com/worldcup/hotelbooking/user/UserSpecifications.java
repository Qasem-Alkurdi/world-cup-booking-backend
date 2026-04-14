package com.worldcup.hotelbooking.user;

import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<AppUser> usernameContains(String username) {
        return (root, query, cb) ->
                username == null || username.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }

    public static Specification<AppUser> emailContains(String email) {
        return (root, query, cb) ->
                email == null || email.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }
}
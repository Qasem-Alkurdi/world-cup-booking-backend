package com.worldcup.hotelbooking.user;

import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

    public static Specification<AppUser> usernameContains(String username) {
        return (root, query, cb) ->
                username == null || username.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }

    public static Specification<AppUser> emailLike(String email) {
        return (root, query, cb) ->
                email == null || email.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<AppUser> hasRole(Role role) {
        return (root, query, cb) ->
                role == null
                        ? null
                        : cb.isMember(role, root.get("roles"));
    }
}
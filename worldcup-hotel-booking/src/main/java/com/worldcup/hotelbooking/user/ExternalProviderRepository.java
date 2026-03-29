package com.worldcup.hotelbooking.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExternalProviderRepository extends JpaRepository<ExternalProvider, Long> {
    Optional<ExternalProvider> findByProviderAndProviderId(Provider provider, String providerId);
}
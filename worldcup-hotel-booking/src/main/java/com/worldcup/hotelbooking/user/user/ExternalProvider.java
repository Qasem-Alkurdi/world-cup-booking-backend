package com.worldcup.hotelbooking.user.user;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "external_providers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ExternalProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;

    @Column(nullable = false)
    private String providerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    public ExternalProvider(Provider provider, String providerId, AppUser user) {
        this.provider = provider;
        this.providerId = providerId;
        this.user = user;
    }
}
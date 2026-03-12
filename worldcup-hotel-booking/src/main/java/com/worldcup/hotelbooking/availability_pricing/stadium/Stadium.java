package com.worldcup.hotelbooking.availability_pricing.stadium;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Table(name = "stadiums")
public class Stadium {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Stadium name is required")
    @Column(nullable = false)
    private String name;

    private String city;

    private Double latitude;

    private Double longitude;

    private Integer capacity;
}
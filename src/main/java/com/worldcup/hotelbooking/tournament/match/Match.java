package com.worldcup.hotelbooking.tournament.match;

import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "matches", indexes = {
        @Index(name = "idx_match_datetime", columnList = "matchDateTime"),
        @Index(name = "idx_match_stadium", columnList = "stadium_id")
})
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String homeTeam;

    @NotBlank
    @Column(nullable = false)
    private String awayTeam;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime matchDateTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStage stage;

    // Link to Stadium – now the single source of truth for location
    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "stadium_id", nullable = false)
    private Stadium stadium;

    // Match importance factors (optional)
    private boolean isOpeningMatch = false;
    private boolean isDerby = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "match_popular_teams", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "team_name")
    private List<String> popularTeams = new ArrayList<>();

    public enum MatchStage {
        GROUP_STAGE_1,
        GROUP_STAGE_2,
        GROUP_STAGE_3,
        ROUND_OF_32,
        ROUND_OF_16,
        QUARTER_FINAL,
        SEMI_FINAL,
        THIRD_PLACE,
        FINAL
    }
}
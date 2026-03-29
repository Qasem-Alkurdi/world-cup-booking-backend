package com.worldcup.hotelbooking.common.seeder;

import com.worldcup.hotelbooking.tournament.match.Match;
import com.worldcup.hotelbooking.tournament.match.MatchRepository;
import com.worldcup.hotelbooking.tournament.match.Match.MatchStage;
import com.worldcup.hotelbooking.tournament.stadium.Stadium;
import com.worldcup.hotelbooking.tournament.stadium.StadiumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Order(2)
@Profile("seed")
@RequiredArgsConstructor
@Slf4j
public class MatchSeeder implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final StadiumRepository stadiumRepository;

    // ---- Helper record for group matches ----
    private record GroupMatch(
            String group,
            String homeTeam,
            String awayTeam,
            int year, Month month, int day, int hour, int minute,
            String stadiumName,
            MatchStage stage,
            boolean isOpening
    ) {}

    // ---- Helper record for knockout matches ----
    private record KnockoutMatch(
            MatchStage stage,
            int year, Month month, int day, int hour, int minute,
            String stadiumName
    ) {}

    @Override
    @Transactional
    public void run(String... args) {
        if (matchRepository.count() > 0) {
            log.info("Matches already exist. Skipping seeder.");
            return;
        }

        List<Stadium> stadiums = stadiumRepository.findAll();
        if (stadiums.isEmpty()) {
            log.warn("No stadiums found. Run StadiumSeeder first.");
            return;
        }

        // Index stadiums by normalized name (lowercase, no spaces/special chars)
        Map<String, Stadium> stadiumMap = stadiums.stream()
                .collect(Collectors.toMap(
                        s -> normalizeName(s.getName()),
                        s -> s
                ));

        List<Match> matches = new ArrayList<>();

        // ---------- GROUP STAGE (72 matches) ----------
        // Official fixtures from FIFA (as of December 2025 draw)
        for (GroupMatch gm : GROUP_MATCHES) {
            Stadium stadium = stadiumMap.get(normalizeName(gm.stadiumName()));
            if (stadium == null) {
                log.error("Stadium '{}' not found. Skipping match: {} vs {}",
                        gm.stadiumName(), gm.homeTeam(), gm.awayTeam());
                continue;
            }
            Match match = new Match();
            match.setHomeTeam(gm.homeTeam());
            match.setAwayTeam(gm.awayTeam());
            match.setMatchDateTime(LocalDateTime.of(gm.year(), gm.month(), gm.day(), gm.hour(), gm.minute()));
            match.setStage(gm.stage());
            match.setStadium(stadium);
            match.setOpeningMatch(gm.isOpening());
            match.setDerby(false);
            match.setPopularTeams(new ArrayList<>());
            matches.add(match);
        }

        // ---------- KNOCKOUT STAGE (32 matches) ----------
        // Placeholder pairings (real teams will be determined after group stage)
        for (KnockoutMatch km : KNOCKOUT_MATCHES) {
            Stadium stadium = stadiumMap.get(normalizeName(km.stadiumName()));
            if (stadium == null) {
                log.error("Stadium '{}' not found. Skipping knockout match.", km.stadiumName());
                continue;
            }
            Match match = new Match();
            match.setHomeTeam("Winner " + (matches.size() + 1));  // placeholder
            match.setAwayTeam("Winner " + (matches.size() + 2)); // placeholder
            match.setMatchDateTime(LocalDateTime.of(km.year(), km.month(), km.day(), km.hour(), km.minute()));
            match.setStage(km.stage());
            match.setStadium(stadium);
            match.setOpeningMatch(false);
            match.setDerby(false);
            match.setPopularTeams(new ArrayList<>());
            matches.add(match);
        }

        matchRepository.saveAll(matches);
        log.info("Seeded {} matches for World Cup 2026.", matches.size());
    }

    private static String normalizeName(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    // ----- GROUP STAGE FIXTURES (72 matches) -----
    private static final List<GroupMatch> GROUP_MATCHES = Arrays.asList(
            // Group A (Mexico City, Guadalajara, Atlanta, Monterrey)
            new GroupMatch("A", "Mexico", "South Africa", 2026, Month.JUNE, 11, 16, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_1, true),
            new GroupMatch("A", "South Korea", "UEFA Playoff D", 2026, Month.JUNE, 12, 22, 0, "Estadio Akron", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("A", "UEFA Playoff D", "South Africa", 2026, Month.JUNE, 18, 12, 0, "Mercedes-Benz Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("A", "Mexico", "South Korea", 2026, Month.JUNE, 18, 21, 0, "Estadio Akron", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("A", "UEFA Playoff D", "Mexico", 2026, Month.JUNE, 24, 21, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("A", "South Africa", "South Korea", 2026, Month.JUNE, 24, 21, 0, "Estadio BBVA", MatchStage.GROUP_STAGE_3, false),

            // Group B (Toronto, San Francisco, Los Angeles, Vancouver)
            new GroupMatch("B", "Canada", "UEFA Playoff A", 2026, Month.JUNE, 12, 15, 0, "BMO Field", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("B", "Qatar", "Switzerland", 2026, Month.JUNE, 13, 15, 0, "Levi's Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("B", "Switzerland", "UEFA Playoff A", 2026, Month.JUNE, 18, 15, 0, "SoFi Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("B", "Canada", "Qatar", 2026, Month.JUNE, 18, 18, 0, "BC Place", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("B", "Switzerland", "Canada", 2026, Month.JUNE, 24, 15, 0, "BC Place", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("B", "UEFA Playoff A", "Qatar", 2026, Month.JUNE, 24, 15, 0, "Lumen Field", MatchStage.GROUP_STAGE_3, false),

            // Group C (New York, Boston, Philadelphia, Miami)
            new GroupMatch("C", "Brazil", "Morocco", 2026, Month.JUNE, 13, 18, 0, "MetLife Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("C", "Haiti", "Scotland", 2026, Month.JUNE, 13, 21, 0, "Gillette Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("C", "Scotland", "Morocco", 2026, Month.JUNE, 19, 18, 0, "Gillette Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("C", "Brazil", "Haiti", 2026, Month.JUNE, 19, 21, 0, "Lincoln Financial Field", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("C", "Scotland", "Brazil", 2026, Month.JUNE, 24, 18, 0, "Hard Rock Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("C", "Morocco", "Haiti", 2026, Month.JUNE, 24, 18, 0, "Mercedes-Benz Stadium", MatchStage.GROUP_STAGE_3, false),

            // Group D (Los Angeles, Vancouver, San Francisco, Los Angeles)
            new GroupMatch("D", "USA", "Paraguay", 2026, Month.JUNE, 12, 18, 0, "SoFi Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("D", "Australia", "UEFA Playoff C", 2026, Month.JUNE, 13, 21, 0, "BC Place", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("D", "UEFA Playoff C", "Paraguay", 2026, Month.JUNE, 19, 18, 0, "Levi's Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("D", "USA", "Australia", 2026, Month.JUNE, 19, 21, 0, "Lumen Field", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("D", "UEFA Playoff C", "USA", 2026, Month.JUNE, 25, 22, 0, "SoFi Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("D", "Paraguay", "Australia", 2026, Month.JUNE, 25, 22, 0, "Levi's Stadium", MatchStage.GROUP_STAGE_3, false),

            // Group E (Houston, Dallas, Kansas City, Houston)
            new GroupMatch("E", "UEFA Playoff B", "Japan", 2026, Month.JUNE, 14, 15, 0, "NRG Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("E", "Ecuador", "Nigeria", 2026, Month.JUNE, 14, 18, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("E", "Nigeria", "Japan", 2026, Month.JUNE, 20, 15, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("E", "UEFA Playoff B", "Ecuador", 2026, Month.JUNE, 20, 18, 0, "NRG Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("E", "Nigeria", "UEFA Playoff B", 2026, Month.JUNE, 26, 15, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("E", "Japan", "Ecuador", 2026, Month.JUNE, 26, 15, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_3, false),

            // Group F (Monterrey, Guadalajara, Mexico City, Monterrey)
            new GroupMatch("F", "Tunisia", "Japan", 2026, Month.JUNE, 14, 21, 0, "Estadio BBVA", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("F", "Colombia", "Iran", 2026, Month.JUNE, 15, 15, 0, "Estadio Akron", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("F", "Iran", "Japan", 2026, Month.JUNE, 20, 21, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("F", "Tunisia", "Colombia", 2026, Month.JUNE, 21, 15, 0, "Estadio BBVA", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("F", "Iran", "Tunisia", 2026, Month.JUNE, 26, 21, 0, "Estadio Akron", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("F", "Japan", "Colombia", 2026, Month.JUNE, 26, 21, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_3, false),

            // Group G (Seattle, Toronto, Vancouver, Seattle)
            new GroupMatch("G", "Portugal", "Uruguay", 2026, Month.JUNE, 15, 18, 0, "Lumen Field", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("G", "Ghana", "Saudi Arabia", 2026, Month.JUNE, 16, 15, 0, "BMO Field", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("G", "Uruguay", "Saudi Arabia", 2026, Month.JUNE, 21, 18, 0, "BC Place", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("G", "Portugal", "Ghana", 2026, Month.JUNE, 21, 21, 0, "Lumen Field", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("G", "Uruguay", "Ghana", 2026, Month.JUNE, 27, 15, 0, "BC Place", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("G", "Saudi Arabia", "Portugal", 2026, Month.JUNE, 27, 15, 0, "BMO Field", MatchStage.GROUP_STAGE_3, false),

            // Group H (Atlanta, Miami, Philadelphia, Atlanta)
            new GroupMatch("H", "Spain", "Croatia", 2026, Month.JUNE, 16, 18, 0, "Mercedes-Benz Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("H", "Senegal", "UEFA Playoff E", 2026, Month.JUNE, 16, 21, 0, "Hard Rock Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("H", "Croatia", "UEFA Playoff E", 2026, Month.JUNE, 22, 15, 0, "Lincoln Financial Field", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("H", "Spain", "Senegal", 2026, Month.JUNE, 22, 18, 0, "Mercedes-Benz Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("H", "Croatia", "Senegal", 2026, Month.JUNE, 27, 21, 0, "Hard Rock Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("H", "UEFA Playoff E", "Spain", 2026, Month.JUNE, 27, 21, 0, "NRG Stadium", MatchStage.GROUP_STAGE_3, false),

            // Group I (Dallas, Kansas City, Houston, Dallas)
            new GroupMatch("I", "Germany", "Chile", 2026, Month.JUNE, 17, 15, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("I", "Cameroon", "UEFA Playoff F", 2026, Month.JUNE, 17, 18, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("I", "Chile", "UEFA Playoff F", 2026, Month.JUNE, 22, 21, 0, "NRG Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("I", "Germany", "Cameroon", 2026, Month.JUNE, 23, 15, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("I", "Chile", "Cameroon", 2026, Month.JUNE, 28, 15, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("I", "UEFA Playoff F", "Germany", 2026, Month.JUNE, 28, 15, 0, "NRG Stadium", MatchStage.GROUP_STAGE_3, false),

            // Group J (New York, Boston, Philadelphia, New York)
            new GroupMatch("J", "France", "Netherlands", 2026, Month.JUNE, 17, 21, 0, "MetLife Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("J", "Algeria", "UEFA Playoff G", 2026, Month.JUNE, 18, 18, 0, "Gillette Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("J", "Netherlands", "UEFA Playoff G", 2026, Month.JUNE, 23, 18, 0, "Lincoln Financial Field", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("J", "France", "Algeria", 2026, Month.JUNE, 23, 21, 0, "MetLife Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("J", "Netherlands", "Algeria", 2026, Month.JUNE, 28, 21, 0, "Gillette Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("J", "UEFA Playoff G", "France", 2026, Month.JUNE, 28, 21, 0, "Lincoln Financial Field", MatchStage.GROUP_STAGE_3, false),

            // Group K (Mexico City, Guadalajara, Monterrey, Mexico City)
            new GroupMatch("K", "England", "Croatia", 2026, Month.JUNE, 17, 15, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("K", "Morocco", "UEFA Playoff H", 2026, Month.JUNE, 18, 15, 0, "Estadio Akron", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("K", "Croatia", "UEFA Playoff H", 2026, Month.JUNE, 23, 15, 0, "Estadio BBVA", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("K", "England", "Morocco", 2026, Month.JUNE, 23, 18, 0, "Estadio Azteca", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("K", "Croatia", "Morocco", 2026, Month.JUNE, 29, 15, 0, "Estadio Akron", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("K", "UEFA Playoff H", "England", 2026, Month.JUNE, 29, 15, 0, "Estadio BBVA", MatchStage.GROUP_STAGE_3, false),

            // Group L (Arlington, Houston, Kansas City, Arlington)
            new GroupMatch("L", "England", "Croatia", 2026, Month.JUNE, 17, 15, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("L", "Morocco", "UEFA Playoff H", 2026, Month.JUNE, 18, 15, 0, "NRG Stadium", MatchStage.GROUP_STAGE_1, false),
            new GroupMatch("L", "Croatia", "UEFA Playoff H", 2026, Month.JUNE, 23, 15, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("L", "England", "Morocco", 2026, Month.JUNE, 23, 18, 0, "AT&T Stadium", MatchStage.GROUP_STAGE_2, false),
            new GroupMatch("L", "Croatia", "Morocco", 2026, Month.JUNE, 29, 15, 0, "NRG Stadium", MatchStage.GROUP_STAGE_3, false),
            new GroupMatch("L", "UEFA Playoff H", "England", 2026, Month.JUNE, 29, 15, 0, "Arrowhead Stadium", MatchStage.GROUP_STAGE_3, false)
    );

    // ----- KNOCKOUT STAGE FIXTURES (32 matches) -----
    private static final List<KnockoutMatch> KNOCKOUT_MATCHES = Arrays.asList(
            // Round of 32 (16 matches)
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JUNE, 29, 17, 0, "Mercedes-Benz Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JUNE, 29, 20, 0, "NRG Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JUNE, 30, 17, 0, "Arrowhead Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JUNE, 30, 20, 0, "AT&T Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  1, 17, 0, "SoFi Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  1, 20, 0, "Levi's Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  2, 17, 0, "Lumen Field"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  2, 20, 0, "BC Place"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  3, 17, 0, "BMO Field"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  3, 20, 0, "Gillette Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  4, 17, 0, "Lincoln Financial Field"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  4, 20, 0, "MetLife Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  5, 17, 0, "Hard Rock Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  5, 20, 0, "Estadio Azteca"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  6, 17, 0, "Estadio Akron"),
            new KnockoutMatch(MatchStage.ROUND_OF_32, 2026, Month.JULY,  6, 20, 0, "Estadio BBVA"),

            // Round of 16 (8 matches)
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY,  8, 17, 0, "AT&T Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY,  8, 20, 0, "NRG Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY,  9, 17, 0, "SoFi Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY,  9, 20, 0, "Levi's Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY, 10, 17, 0, "MetLife Stadium"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY, 10, 20, 0, "Lincoln Financial Field"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY, 11, 17, 0, "Estadio Azteca"),
            new KnockoutMatch(MatchStage.ROUND_OF_16, 2026, Month.JULY, 11, 20, 0, "Estadio BBVA"),

            // Quarter-finals (4 matches)
            new KnockoutMatch(MatchStage.QUARTER_FINAL, 2026, Month.JULY, 13, 17, 0, "AT&T Stadium"),
            new KnockoutMatch(MatchStage.QUARTER_FINAL, 2026, Month.JULY, 13, 20, 0, "SoFi Stadium"),
            new KnockoutMatch(MatchStage.QUARTER_FINAL, 2026, Month.JULY, 14, 17, 0, "MetLife Stadium"),
            new KnockoutMatch(MatchStage.QUARTER_FINAL, 2026, Month.JULY, 14, 20, 0, "Estadio Azteca"),

            // Semi-finals (2 matches)
            new KnockoutMatch(MatchStage.SEMI_FINAL, 2026, Month.JULY, 16, 17, 0, "AT&T Stadium"),
            new KnockoutMatch(MatchStage.SEMI_FINAL, 2026, Month.JULY, 17, 17, 0, "SoFi Stadium"),

            // Third place
            new KnockoutMatch(MatchStage.THIRD_PLACE, 2026, Month.JULY, 18, 17, 0, "Hard Rock Stadium"),

            // Final
            new KnockoutMatch(MatchStage.FINAL, 2026, Month.JULY, 19, 18, 0, "MetLife Stadium")
    );
}
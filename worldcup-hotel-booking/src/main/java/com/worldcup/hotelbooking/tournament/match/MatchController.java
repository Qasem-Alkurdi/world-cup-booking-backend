package com.worldcup.hotelbooking.tournament.match;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
@Tag(name = "Matches", description = "Public match information and admin management")
public class MatchController {

    private final MatchService matchService;

    // Public read endpoints

    @GetMapping
    @Operation(summary = "Get all matches with pagination and optional stage filter")
    public ResponseEntity<Page<Match>> getAllMatches(
            @ParameterObject @PageableDefault(size = 20, sort = "matchDateTime") Pageable pageable,
            @RequestParam(required = false) Match.MatchStage stage) {

        Page<Match> page = matchService.getAllMatches(pageable, stage);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get match by ID")
    public ResponseEntity<Match> getMatchById(@PathVariable Long id) {
        return ResponseEntity.ok(matchService.getMatchById(id));
    }

    @GetMapping("/stadium/{stadiumId}")
    public ResponseEntity<Page<Match>> getMatchesByStadium(
            @PathVariable Long stadiumId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<Match> page = matchService.getMatchesByStadium(stadiumId, pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/between")
    @Operation(summary = "Get matches between two dates")
    public ResponseEntity<List<Match>> getMatchesBetweenDates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(matchService.getMatchesBetweenDates(start, end));
    }

    @GetMapping("/city/{city}/between")
    @Operation(summary = "Get matches in a specific city between two dates")
    public ResponseEntity<List<Match>> getMatchesInCityBetweenDates(
            @PathVariable String city,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(matchService.getMatchesInCityBetweenDates(city, start, end));
    }

    // Admin-only write endpoints

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new match (Admin only)")
    public ResponseEntity<Match> createMatch(@Valid @RequestBody Match match) {
        Match created = matchService.createMatch(match);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing match (Admin only)")
    public ResponseEntity<Match> updateMatch(@PathVariable Long id, @Valid @RequestBody Match match) {
        Match updated = matchService.updateMatch(id, match);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a match (Admin only)")
    public ResponseEntity<Void> deleteMatch(@PathVariable Long id) {
        matchService.deleteMatch(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/search")
    @Operation(summary = "Search matches with multiple filters and pagination")
    public ResponseEntity<Page<Match>> searchMatches(
            @RequestParam(required = false) Match.MatchStage stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long stadiumId,
            @RequestParam(required = false) String team,
            @RequestParam(required = false) Boolean derby,
            @RequestParam(required = false) Boolean opening,
            @PageableDefault(size = 20, sort = "matchDateTime") Pageable pageable) {

        Page<Match> result = matchService.searchMatches(
                stage, startDate, endDate, city, stadiumId, team, derby, opening, pageable);
        return ResponseEntity.ok(result);
    }
}
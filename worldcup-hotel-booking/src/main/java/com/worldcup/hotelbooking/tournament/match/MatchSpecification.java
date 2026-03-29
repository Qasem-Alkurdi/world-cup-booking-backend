package com.worldcup.hotelbooking.tournament.match;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class MatchSpecification {

    public static Specification<Match> hasStage(Match.MatchStage stage) {
        return (root, query, cb) -> stage == null ? null : cb.equal(root.get("stage"), stage);
    }

    public static Specification<Match> dateBetween(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return null;
            if (start != null && end != null)
                return cb.between(root.get("matchDateTime"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(root.get("matchDateTime"), start);
            return cb.lessThanOrEqualTo(root.get("matchDateTime"), end);
        };
    }

    public static Specification<Match> hasCity(String city) {
        return (root, query, cb) -> city == null ? null :
                cb.equal(root.get("stadium").get("city"), city);
    }

    public static Specification<Match> hasStadiumId(Long stadiumId) {
        return (root, query, cb) -> stadiumId == null ? null :
                cb.equal(root.get("stadium").get("id"), stadiumId);
    }

    public static Specification<Match> teamContains(String team) {
        return (root, query, cb) -> {
            if (team == null) return null;
            String pattern = "%" + team.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("homeTeam")), pattern),
                    cb.like(cb.lower(root.get("awayTeam")), pattern)
            );
        };
    }

    public static Specification<Match> isDerby(Boolean derby) {
        return (root, query, cb) -> derby == null ? null : cb.equal(root.get("isDerby"), derby);
    }

    public static Specification<Match> isOpeningMatch(Boolean opening) {
        return (root, query, cb) -> opening == null ? null : cb.equal(root.get("isOpeningMatch"), opening);
    }
}
// com.worldcup.hotelbooking.user.user.PasswordValidator.java
package com.worldcup.hotelbooking.user;

import com.nulabinc.zxcvbn.Strength;
import com.nulabinc.zxcvbn.Zxcvbn;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class PasswordValidator {

    private final Set<String> commonPasswords;
    private final Zxcvbn zxcvbn;
    private final RestTemplate restTemplate;
    @Value("${password.min-length:12}")
    private int minLength;

    public PasswordValidator(RestTemplate restTemplate) throws IOException {
        this.restTemplate = restTemplate;
        this.commonPasswords = loadCommonPasswords();
        this.zxcvbn = new Zxcvbn();
    }

    /**
     * Validate password strength. Throws PasswordValidationException if any rule fails.
     */
    public void validate(String password) {
        List<String> issues = new ArrayList<>();

        issues.addAll(checkBasicRules(password));
        issues.addAll(checkPatterns(password));
        issues.addAll(checkDictionary(password));
        issues.addAll(checkZxcvbn(password));
        issues.addAll(checkBreach(password));

        if (!issues.isEmpty()) {
            throw new PasswordValidationException(issues);
        }
    }

    private Set<String> loadCommonPasswords() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/common-passwords.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines()
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
    }

    private List<String> checkBasicRules(String password) {
        List<String> issues = new ArrayList<>();
        if (password.length() < minLength) {
            issues.add("Password must be at least " + minLength + " characters");
        }
        if (!password.matches(".*[A-Z].*")) {
            issues.add("Must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            issues.add("Must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            issues.add("Must contain at least one digit");
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            issues.add("Must contain at least one special character");
        }
        return issues;
    }

    private List<String> checkPatterns(String password) {
        List<String> issues = new ArrayList<>();
        String lower = password.toLowerCase();

        // Repeated characters (3 or more same chars in a row)
        if (password.matches(".*(.)\\1{2,}.*")) {
            issues.add("Contains three or more identical characters in a row");
        }

        // Sequential patterns (common sequences of 4 or more)
        List<String> sequences = List.of(
                "abcdefghijklmnopqrstuvwxyz",
                "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                "0123456789",
                "1234567890"
        );
        for (String seq : sequences) {
            for (int i = 0; i <= seq.length() - 4; i++) {
                if (lower.contains(seq.substring(i, i + 4).toLowerCase())) {
                    issues.add("Contains a sequential pattern (e.g., " + seq.substring(i, i + 4) + ")");
                    break;
                }
            }
        }

        // Keyboard rows
        List<String> rows = List.of(
                "qwertyuiop", "asdfghjkl", "zxcvbnm",
                "QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM"
        );
        for (String row : rows) {
            for (int i = 0; i <= row.length() - 4; i++) {
                if (lower.contains(row.substring(i, i + 4).toLowerCase())) {
                    issues.add("Contains a keyboard pattern (e.g., " + row.substring(i, i + 4) + ")");
                    break;
                }
            }
        }

        // Common substitutions – we rely on zxcvbn for this (it will catch "P@ssw0rd")
        return issues;
    }

    private List<String> checkDictionary(String password) {
        if (commonPasswords.contains(password.toLowerCase())) {
            return List.of("Password is too common");
        }
        return Collections.emptyList();
    }

    private List<String> checkZxcvbn(String password) {
        Strength strength = zxcvbn.measure(password);
        int score = strength.getScore(); // 0-4
        List<String> issues = new ArrayList<>();
        if (score < 3) {
            issues.add("Password is not strong enough (score: " + score + "/4)");
        }
        // Add zxcvbn's warnings and suggestions as issues
        if (strength.getFeedback() != null) {
            if (StringUtils.isNotBlank(strength.getFeedback().getWarning())) {
                issues.add(strength.getFeedback().getWarning());
            }
            issues.addAll(strength.getFeedback().getSuggestions());
        }
        return issues;
    }

    private List<String> checkBreach(String password) {
        if (isBreached(password)) {
            return List.of("This password has appeared in a known data breach");
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "hibpSuffixes", key = "#prefix")
    public Set<String> fetchSuffixes(String prefix) {
        String url = "https://api.pwnedpasswords.com/range/" + prefix;
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return Arrays.stream(response.getBody().split("\\r?\\n"))
                    .map(line -> line.split(":")[0].trim().toUpperCase())
                    .collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    private boolean isBreached(String password) {
        try {
            String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(password).toUpperCase();
            String prefix = sha1.substring(0, 5);
            String suffix = sha1.substring(5);
            Set<String> suffixes = fetchSuffixes(prefix);
            return suffixes.contains(suffix);
        } catch (Exception e) {
            // Log error but treat as not breached to avoid blocking registration
            return false;
        }
    }
}
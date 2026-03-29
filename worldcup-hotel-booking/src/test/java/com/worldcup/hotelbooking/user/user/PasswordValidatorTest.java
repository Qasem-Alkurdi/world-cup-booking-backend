package com.worldcup.hotelbooking.user.user;

import com.worldcup.hotelbooking.user.PasswordValidationException;
import com.worldcup.hotelbooking.user.PasswordValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordValidatorTest {

    @Mock
    private RestTemplate restTemplate;

    private PasswordValidator validator;

    @BeforeEach
    void setUp() throws Exception {
        validator = new PasswordValidator(restTemplate);

        // Override commonPasswords with a test set
        Set<String> testPasswords = Set.of("password", "123456", "qwerty");
        java.lang.reflect.Field commonField = PasswordValidator.class.getDeclaredField("commonPasswords");
        commonField.setAccessible(true);
        commonField.set(validator, testPasswords);

        // Override minLength to 12 (since @Value is not processed in pure Mockito test)
        java.lang.reflect.Field minLengthField = PasswordValidator.class.getDeclaredField("minLength");
        minLengthField.setAccessible(true);
        minLengthField.setInt(validator, 12);
    }

    @Test
    void shouldAcceptStrongPassword() {
        String strongPassword = "MySecureP@ssw0rd2025!";
        assertDoesNotThrow(() -> validator.validate(strongPassword));
    }

    @Test
    void shouldRejectShortPassword() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("Short1!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("12 characters")));
    }

    @Test
    void shouldRejectMissingUppercase() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("nouppercase1!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("uppercase")));
    }

    @Test
    void shouldRejectMissingDigit() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("NoDigitHere!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("digit")));
    }

    @Test
    void shouldRejectMissingSpecialChar() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("NoSpecialChar123"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("special")));
    }

    @Test
    void shouldRejectRepeatedCharacters() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("aaaaBcD1!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("identical characters")));
    }

    @Test
    void shouldRejectSequentialPattern() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("abcdBcD1!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("sequential")));
    }

    @Test
    void shouldRejectKeyboardPattern() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("qwerty1!"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("keyboard")));
    }

    @Test
    void shouldRejectCommonPassword() {
        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate("password"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("common")));
    }

    @Test
    void shouldRejectBreachedPassword() {
        String breachedPassword = "Breached123!";
        String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(breachedPassword).toUpperCase();
        String suffix = sha1.substring(5);
        String mockResponse = suffix + ":5\n" + "OTHERSUFFIX:2\n";
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(mockResponse));

        PasswordValidationException ex = assertThrows(PasswordValidationException.class,
                () -> validator.validate(breachedPassword));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("breach")));
    }

    @Test
    void shouldNotRejectWhenHibpFails() {
        when(restTemplate.getForEntity(anyString(), eq(String.class)))
                .thenThrow(new RuntimeException("Network error"));

        String password = "MySecureP@ssw0rd2025!";
        assertDoesNotThrow(() -> validator.validate(password));
    }
}
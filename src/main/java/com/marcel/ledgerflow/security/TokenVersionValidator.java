
package com.marcel.ledgerflow.security;

import com.marcel.ledgerflow.entity.User;
import com.marcel.ledgerflow.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class TokenVersionValidator
        implements OAuth2TokenValidator<Jwt> {

    private final UserRepository userRepository;

    public TokenVersionValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {

        String subject = jwt.getSubject();

        if (subject == null || !subject.matches("\\d+")) {
            return invalidToken();
        }

        Long userId;

        try {
            userId = Long.valueOf(subject);
        } catch (NumberFormatException exception) {
            return invalidToken();
        }

        Object versionClaim = jwt.getClaims().get("tokenVersion");

        if (!(versionClaim instanceof Number number)) {
            return invalidToken();
        }

        long tokenVersion;

        try {
            BigDecimal version =
                    new BigDecimal(number.toString());

            tokenVersion = version.longValueExact();

            if (tokenVersion < 0) {
                return invalidToken();
            }

        } catch (NumberFormatException | ArithmeticException exception) {
            return invalidToken();
        }

        Optional<User> user = userRepository.findById(userId);

        if (user.isEmpty()
                || user.get().getTokenVersion() == null
                || user.get().getTokenVersion() != tokenVersion) {
            return invalidToken();
        }

        return OAuth2TokenValidatorResult.success();
    }

    private OAuth2TokenValidatorResult invalidToken() {
        OAuth2Error error = new OAuth2Error(
                "invalid_token",
                "Token has been revoked or is invalid",
                null
        );

        return OAuth2TokenValidatorResult.failure(error);
    }
}

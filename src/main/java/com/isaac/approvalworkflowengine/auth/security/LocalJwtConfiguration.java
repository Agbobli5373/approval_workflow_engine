package com.isaac.approvalworkflowengine.auth.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@ConditionalOnProperty(prefix = "app.security", name = "mode", havingValue = "LOCAL_AUTH", matchIfMissing = true)
public class LocalJwtConfiguration {

    @Bean
    SecretKey localAuthSecretKey(AppSecurityProperties properties) {
        byte[] keyBytes = properties.getJwt().getHmacSecret().getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey localAuthSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(localAuthSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey localAuthSecretKey) {
        return NimbusJwtDecoder.withSecretKey(localAuthSecretKey)
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

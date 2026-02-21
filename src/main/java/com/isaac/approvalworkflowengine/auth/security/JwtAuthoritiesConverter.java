package com.isaac.approvalworkflowengine.auth.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthoritiesConverter implements Converter<Jwt, JwtAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter scopeAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>(scopeAuthoritiesConverter.convert(jwt));
        authorities.addAll(extractRoleAuthorities(jwt));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    private Collection<? extends GrantedAuthority> extractRoleAuthorities(Jwt jwt) {
        Set<String> roles = extractRoles(jwt);
        return roles.stream()
            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());
    }

    private Set<String> extractRoles(Jwt jwt) {
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof Collection<?> roleCollection) {
            return roleCollection.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .map(role -> role.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        }

        Object realmAccessClaim = jwt.getClaims().get("realm_access");
        if (realmAccessClaim instanceof Map<?, ?> realmAccessMap) {
            Object realmRoles = realmAccessMap.get("roles");
            if (realmRoles instanceof Collection<?> roleCollection) {
                return roleCollection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            }
        }

        return Set.of();
    }
}

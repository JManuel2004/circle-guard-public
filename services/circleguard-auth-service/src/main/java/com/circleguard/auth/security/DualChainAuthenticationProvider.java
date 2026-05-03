package com.circleguard.auth.security;

import com.circleguard.auth.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.*;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DualChainAuthenticationProvider implements AuthenticationProvider {

    private final LdapAuthenticationProvider ldapProvider;
    private final DaoAuthenticationProvider localProvider;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        try {
            // Chain 1: Try LDAP
            return ldapProvider.authenticate(authentication);
        } catch (AuthenticationException e) {
            // Chain 2: Fallback to Local DB
            return localProvider.authenticate(authentication);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}

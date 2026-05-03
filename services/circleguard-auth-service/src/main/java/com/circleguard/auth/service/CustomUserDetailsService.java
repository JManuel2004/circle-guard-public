package com.circleguard.auth.service;

import com.circleguard.auth.model.*;
import com.circleguard.auth.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final LocalUserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Looking up user in database: " + username);
        LocalUser user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    System.err.println("User NOT found in database: " + username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        System.out.println("User found: " + user.getUsername() + ", active: " + user.getIsActive());

        if (!user.getIsActive()) {
            System.err.println("User account is DISABLED: " + username);
            throw new DisabledException("User account is disabled");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        
        // Add roles (prefixed with ROLE_)
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            
            // Add granular permissions
            role.getPermissions().forEach(permission -> {
                authorities.add(new SimpleGrantedAuthority(permission.getName()));
            });
        });

        return new User(
                user.getUsername(),
                user.getPassword(),
                authorities
        );
    }
}

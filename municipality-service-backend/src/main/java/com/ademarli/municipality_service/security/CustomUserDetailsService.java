package com.ademarli.municipality_service.security;

import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrId) {
        User user = findUser(usernameOrId);

        return new org.springframework.security.core.userdetails.User(
                user.getId().toString(),
                user.getPasswordHash(),
                user.isEnabled(),
                true, true, true,
                user.getRoles().stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.name()))
                        .collect(Collectors.toSet())
        );
    }

    private User findUser(String usernameOrId) {
        // 1) id mi?
        try {
            Long id = Long.parseLong(usernameOrId);
            return userRepository.findById(id)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found by id: " + usernameOrId));
        } catch (NumberFormatException ignored) {}

        // 2) email mi?
        return userRepository.findByEmail(usernameOrId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found by email: " + usernameOrId));
    }
}

package com.ademarli.municipality_service.unit;

import com.ademarli.municipality_service.exception.BusinessException;
import com.ademarli.municipality_service.exception.NotFoundException;
import com.ademarli.municipality_service.model.dto.auth.AuthResponse;
import com.ademarli.municipality_service.model.dto.auth.LoginRequest;
import com.ademarli.municipality_service.model.dto.auth.RegisterRequest;
import com.ademarli.municipality_service.model.entity.User;
import com.ademarli.municipality_service.model.enums.Role;
import com.ademarli.municipality_service.repository.UserRepository;
import com.ademarli.municipality_service.security.JwtUtil;
import com.ademarli.municipality_service.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtUtil jwtUtil;

    @InjectMocks AuthService service;

    private RegisterRequest registerReq(String email, String phone, String password) {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(email);
        r.setPhone(phone);
        r.setPassword(password);
        return r;
    }

    private LoginRequest loginReq(String emailOrPhone, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmailOrPhone(emailOrPhone);
        r.setPassword(password);
        return r;
    }

    private User user(Long id, String email, String phone, String hash, boolean enabled, Role... roles) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setPhone(phone);
        u.setPasswordHash(hash);
        u.setEnabled(enabled);
        if (roles != null) u.setRoles(new HashSet<>(Arrays.asList(roles)));
        return u;
    }

    // register

    @Test
    void register_whenEmailAlreadyInUse_shouldThrow() {
        RegisterRequest req = registerReq("  a@a.com  ", null, "pw");

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(new User()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(req));
        assertEquals("EMAIL_ALREADY_IN_USE", ex.getMessage());

        verify(userRepository).findByEmail("a@a.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_whenPhoneAlreadyInUse_shouldThrow() {
        RegisterRequest req = registerReq(null, "  0555  ", "pw");

        when(userRepository.findByPhone("0555")).thenReturn(Optional.of(new User()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.register(req));
        assertEquals("PHONE_ALREADY_IN_USE", ex.getMessage());

        verify(userRepository).findByPhone("0555");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ok_shouldTrimEncodeSetDefaultsSaveAndReturnToken() {
        RegisterRequest req = registerReq("  User@Mail.com  ", "  0555  ", "pw");

        when(userRepository.findByEmail("User@Mail.com")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0555")).thenReturn(Optional.empty());

        when(passwordEncoder.encode("pw")).thenReturn("HASH");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        when(jwtUtil.generateToken(eq(10L), eq("User@Mail.com"), anyList())).thenReturn("JWT");
        when(jwtUtil.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse out = service.register(req);

        assertNotNull(out);
        assertEquals("JWT", out.accessToken());
        assertEquals("Bearer", out.tokenType());
        assertEquals(3600L, out.expiresInSeconds());

        verify(passwordEncoder).encode("pw");

        verify(userRepository).save(argThat(u ->
                u.getId() != null && u.getId().equals(10L) &&
                        "User@Mail.com".equals(u.getEmail()) &&
                        "0555".equals(u.getPhone()) &&
                        "HASH".equals(u.getPasswordHash()) &&
                        u.isEnabled() &&
                        u.getRoles() != null &&
                        u.getRoles().contains(Role.CITIZEN) &&
                        u.getRoles().size() == 1
        ));

        verify(jwtUtil).generateToken(eq(10L), eq("User@Mail.com"),
                argThat(list -> list.contains("CITIZEN") && list.size() == 1));
        verify(jwtUtil).getExpirationSeconds();
    }

    @Test
    void register_ok_whenEmailAndPhoneNull_shouldStillRegisterAndIssueToken() {
        RegisterRequest req = registerReq(null, null, "pw");

        when(passwordEncoder.encode("pw")).thenReturn("HASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(11L);
            return u;
        });

        when(jwtUtil.generateToken(eq(11L), isNull(), anyList())).thenReturn("JWT2");
        when(jwtUtil.getExpirationSeconds()).thenReturn(7200L);

        AuthResponse out = service.register(req);

        assertEquals("JWT2", out.accessToken());
        assertEquals("Bearer", out.tokenType());
        assertEquals(7200L, out.expiresInSeconds());

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhone(anyString());

        verify(userRepository).save(argThat(u ->
                u.getEmail() == null &&
                        u.getPhone() == null &&
                        "HASH".equals(u.getPasswordHash()) &&
                        u.isEnabled() &&
                        u.getRoles() != null &&
                        u.getRoles().contains(Role.CITIZEN) &&
                        u.getRoles().size() == 1
        ));
    }

    // login

    @Test
    void login_whenNullOrBlankEmailOrPhone_shouldThrowInvalidCredentials() {
        BusinessException ex1 = assertThrows(BusinessException.class, () -> service.login(loginReq(null, "pw")));
        assertEquals("INVALID_CREDENTIALS", ex1.getMessage());

        BusinessException ex2 = assertThrows(BusinessException.class, () -> service.login(loginReq("   ", "pw")));
        assertEquals("INVALID_CREDENTIALS", ex2.getMessage());

        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhone(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyLong(), any(), anyList());
    }

    @Test
    void login_whenContainsAtSign_shouldLowercaseEmailKey_andUseFindByEmail() {
        User u = user(5L, "TeSt@Mail.com", null, "HASH", true, Role.CITIZEN);

        when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);
        when(jwtUtil.generateToken(eq(5L), eq("TeSt@Mail.com"), anyList())).thenReturn("JWT");
        when(jwtUtil.getExpirationSeconds()).thenReturn(1000L);

        AuthResponse out = service.login(loginReq("  TeSt@Mail.com  ", "pw"));

        assertEquals("JWT", out.accessToken());
        assertEquals("Bearer", out.tokenType());
        assertEquals(1000L, out.expiresInSeconds());

        verify(userRepository).findByEmail("test@mail.com");
        verify(userRepository, never()).findByPhone(anyString());
    }

    @Test
    void login_whenNoAtSignAndEmailNotFound_shouldFallbackToPhone() {
        User u = user(6L, null, "0555", "HASH", true, Role.CITIZEN);

        when(userRepository.findByEmail("0555")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("0555")).thenReturn(Optional.of(u));

        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);
        when(jwtUtil.generateToken(eq(6L), isNull(), anyList())).thenReturn("JWT6");
        when(jwtUtil.getExpirationSeconds()).thenReturn(1111L);

        AuthResponse out = service.login(loginReq(" 0555 ", "pw"));

        assertEquals("JWT6", out.accessToken());
        assertEquals(1111L, out.expiresInSeconds());

        verify(userRepository).findByEmail("0555");
        verify(userRepository).findByPhone("0555");
    }

    @Test
    void login_whenUserNotFound_shouldThrow() {
        when(userRepository.findByEmail("x")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("x")).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () ->
                service.login(loginReq("x", "pw"))
        );
        assertEquals("USER_NOT_FOUND", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyLong(), any(), anyList());
    }

    @Test
    void login_whenUserDisabled_shouldThrow() {
        User u = user(7L, "a@a.com", null, "HASH", false, Role.CITIZEN);

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.login(loginReq("a@a.com", "pw"))
        );
        assertEquals("USER_DISABLED", ex.getMessage());

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyLong(), any(), anyList());
    }

    @Test
    void login_whenInvalidPassword_shouldThrow() {
        User u = user(8L, "a@a.com", null, "HASH", true, Role.CITIZEN);

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.login(loginReq("a@a.com", "wrong"))
        );
        assertEquals("INVALID_PASSWORD", ex.getMessage());

        verify(passwordEncoder).matches("wrong", "HASH");
        verify(jwtUtil, never()).generateToken(anyLong(), any(), anyList());
    }

    @Test
    void login_ok_shouldIssueTokenWithRolesList() {
        User u = user(9L, "a@a.com", null, "HASH", true, Role.CITIZEN, Role.AGENT);

        when(userRepository.findByEmail("a@a.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("pw", "HASH")).thenReturn(true);

        when(jwtUtil.generateToken(eq(9L), eq("a@a.com"),
                argThat(list -> list.containsAll(List.of("CITIZEN", "AGENT")) && list.size() == 2)))
                .thenReturn("JWT9");
        when(jwtUtil.getExpirationSeconds()).thenReturn(2222L);

        AuthResponse out = service.login(loginReq("a@a.com", "pw"));

        assertEquals("JWT9", out.accessToken());
        assertEquals("Bearer", out.tokenType());
        assertEquals(2222L, out.expiresInSeconds());

        verify(jwtUtil).getExpirationSeconds();
    }
}

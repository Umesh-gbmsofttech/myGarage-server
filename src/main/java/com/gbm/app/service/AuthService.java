package com.gbm.app.service;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gbm.app.dto.AuthRequest;
import com.gbm.app.dto.AuthResponse;
import com.gbm.app.dto.SignupMechanicRequest;
import com.gbm.app.dto.SignupOwnerRequest;
import com.gbm.app.entity.AuthSession;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.entity.VehicleOwnerProfile;
import com.gbm.app.repository.AuthSessionRepository;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.repository.UserRepository;
import com.gbm.app.repository.VehicleOwnerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_CACHE_TTL_MS = 60_000L;
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final VehicleOwnerProfileRepository vehicleOwnerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConcurrentHashMap<String, CachedUserEntry> tokenUserCache = new ConcurrentHashMap<>();

    public AuthResponse signupMechanic(SignupMechanicRequest request) {
        log.info("Signup mechanic request: mechName={}, surname={}, email={}, mobile={}, city={}, speciality={}, experience={}, shopAct={}",
            request.getMechName(), request.getSurname(), request.getEmail(), request.getMobile(),
            request.getCity(), request.getSpeciality(), request.getExperience(), request.getShopAct());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        validateRole(request.getRole(), Role.MECHANIC);
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getMechName());
        user.setSurname(request.getSurname());
        user.setMobile(request.getMobile());
        user.setRole(Role.MECHANIC);
        user.setUsername(generateUsername(request.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        MechanicProfile profile = new MechanicProfile();
        profile.setUser(user);
        profile.setExperience(request.getExperience());
        profile.setSpeciality(request.getSpeciality());
        profile.setExpertise(request.getSpeciality());
        profile.setCity(request.getCity());
        boolean shopActive = request.getShopAct() != null ? request.getShopAct() : Boolean.TRUE.equals(request.getShopActive());
        profile.setShopActive(shopActive);

        mechanicProfileRepository.save(profile);
        log.info("Created mechanic user id={}", user.getId());
        return buildAuthResponse(user, createSession(user));
    }

    public AuthResponse signupOwner(SignupOwnerRequest request) {
        log.info("Signup owner request: name={}, surname={}, email={}, mobile={}, role={}",
            request.getName(), request.getSurname(), request.getEmail(), request.getMobile(), request.getRole());
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }

        validateOwnerRole(request.getRole());
        User user = new User();
        user.setEmail(request.getEmail());
        user.setFirstName(request.getName());
        user.setSurname(request.getSurname());
        user.setMobile(request.getMobile());
        user.setRole(Role.VEHICLE_OWNER);
        user.setUsername(generateUsername(request.getEmail()));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        VehicleOwnerProfile profile = new VehicleOwnerProfile();
        profile.setUser(user);

        vehicleOwnerProfileRepository.save(profile);
        log.info("Created owner user id={}", user.getId());
        return buildAuthResponse(user, createSession(user));
    }

    public AuthResponse signin(AuthRequest request) {
        log.info("Signin request: email={}", request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        AuthSession session = createSession(user);
        log.info("Signin success user id={}", user.getId());
        return buildAuthResponse(user, session);
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorizationHeader) {
        String token = resolveToken(authorizationHeader);
        if (token == null) {
            throw new IllegalArgumentException("Missing token");
        }

        Instant now = Instant.now();
        CachedUserEntry cached = tokenUserCache.get(token);
        if (cached != null) {
            if (cached.cacheExpiresAt.isAfter(now) && cached.sessionExpiresAt.isAfter(now)) {
                return cached.user;
            }
            tokenUserCache.remove(token, cached);
        }

        AuthSession session = authSessionRepository.findByToken(token)
            .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (session.getExpiresAt().isBefore(now)) {
            throw new IllegalArgumentException("Token expired");
        }
        cacheTokenUser(token, session.getUser(), session.getExpiresAt(), now);
        return session.getUser();
    }

    private AuthSession createSession(User user) {
        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setToken(UUID.randomUUID().toString());
        session.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        AuthSession saved = authSessionRepository.save(session);
        cacheTokenUser(saved.getToken(), user, saved.getExpiresAt(), Instant.now());
        return saved;
    }

    private AuthResponse buildAuthResponse(User user, AuthSession session) {
        AuthResponse response = new AuthResponse();
        response.setUserId(user.getId());
        response.setToken(session.getToken());
        response.setRole(user.getRole());
        response.setName(buildName(user));
        response.setFirstName(user.getFirstName());
        response.setSurname(user.getSurname());
        response.setEmail(user.getEmail());
        return response;
    }

    private String buildName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName();
        String last = user.getSurname() == null ? "" : user.getSurname();
        return (first + " " + last).trim();
    }

    private void validateRole(String role, Role expected) {
        if (role == null || role.isBlank()) {
            return;
        }
        if (!role.equalsIgnoreCase(expected.name())) {
            throw new IllegalArgumentException("Invalid role");
        }
    }

    private void validateOwnerRole(String role) {
        if (role == null || role.isBlank()) {
            return;
        }
        if (!(role.equalsIgnoreCase("OWNER") || role.equalsIgnoreCase(Role.VEHICLE_OWNER.name()))) {
            throw new IllegalArgumentException("Invalid role");
        }
    }

    private String generateUsername(String email) {
        String base = email.split("@")[0];
        String username = base;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + counter;
            counter++;
        }
        return username;
    }

    private String resolveToken(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        if (header.toLowerCase().startsWith("bearer ")) {
            return header.substring(7).trim();
        }
        return header.trim();
    }

    private void cacheTokenUser(String token, User user, Instant sessionExpiresAt, Instant now) {
        long ttlMs = Math.min(TOKEN_CACHE_TTL_MS, Math.max(0L, Duration.between(now, sessionExpiresAt).toMillis()));
        if (ttlMs <= 0L) {
            return;
        }
        tokenUserCache.put(token, new CachedUserEntry(user, sessionExpiresAt, now.plusMillis(ttlMs)));
    }

    private static class CachedUserEntry {
        private final User user;
        private final Instant sessionExpiresAt;
        private final Instant cacheExpiresAt;

        private CachedUserEntry(User user, Instant sessionExpiresAt, Instant cacheExpiresAt) {
            this.user = user;
            this.sessionExpiresAt = sessionExpiresAt;
            this.cacheExpiresAt = cacheExpiresAt;
        }
    }
}

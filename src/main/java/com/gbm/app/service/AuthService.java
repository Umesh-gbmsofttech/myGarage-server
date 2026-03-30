package com.gbm.app.service;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gbm.app.dto.AuthRequest;
import com.gbm.app.dto.AuthResponse;
import com.gbm.app.dto.ForgotPasswordRequest;
import com.gbm.app.dto.ResetPasswordRequest;
import com.gbm.app.dto.SignupMechanicRequest;
import com.gbm.app.dto.SignupOwnerRequest;
import com.gbm.app.dto.VerifyOtpRequest;
import com.gbm.app.entity.ApprovalStatus;
import com.gbm.app.entity.AuthSession;
import com.gbm.app.entity.MechanicProfile;
import com.gbm.app.entity.MechanicRegistrationSource;
import com.gbm.app.entity.PasswordResetOtp;
import com.gbm.app.entity.Role;
import com.gbm.app.entity.User;
import com.gbm.app.entity.VehicleOwnerProfile;
import com.gbm.app.repository.AuthSessionRepository;
import com.gbm.app.repository.MechanicProfileRepository;
import com.gbm.app.repository.PasswordResetOtpRepository;
import com.gbm.app.repository.UserRepository;
import com.gbm.app.repository.VehicleOwnerProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long TOKEN_CACHE_TTL_MS = 60_000L;
    private static final Pattern PASSWORD_RULE =
        Pattern.compile("^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");
    private final UserRepository userRepository;
    private final AuthSessionRepository authSessionRepository;
    private final MechanicProfileRepository mechanicProfileRepository;
    private final VehicleOwnerProfileRepository vehicleOwnerProfileRepository;
    private final PasswordResetOtpRepository passwordResetOtpRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ConcurrentHashMap<String, CachedUserEntry> tokenUserCache = new ConcurrentHashMap<>();

    public AuthResponse signupMechanic(SignupMechanicRequest request) {
        log.info("Signup mechanic request: mechName={}, surname={}, email={}, mobile={}, city={}, speciality={}, experience={}",
            request.getMechName(), request.getSurname(), request.getEmail(), request.getMobile(),
            request.getCity(), request.getSpeciality(), request.getExperience());
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
        validatePassword(request.getPassword());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        MechanicProfile profile = new MechanicProfile();
        profile.setUser(user);
        profile.setExperience(request.getExperience());
        profile.setSpeciality(request.getSpeciality());
        profile.setExpertise(request.getSpeciality());
        profile.setCity(request.getCity());
        profile.setCertificate(request.getCertificate());
        profile.setApprovalStatus(ApprovalStatus.PENDING);
        profile.setRegistrationSource(MechanicRegistrationSource.SELF);

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
        validatePassword(request.getPassword());
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

    public Map<String, Object> sendForgotPasswordOtp(ForgotPasswordRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            PasswordResetOtp otp = new PasswordResetOtp();
            otp.setUser(user);
            otp.setOtp(generateOtp());
            otp.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
            passwordResetOtpRepository.save(otp);
            emailService.sendOtpEmail(email, otp.getOtp());
        });
        return Map.of("message", "If the email exists, an OTP has been sent.");
    }

    public Map<String, Object> verifyOtp(VerifyOtpRequest request) {
        PasswordResetOtp otp = resolveOtp(request.getEmail(), request.getOtp());
        otp.setVerifiedAt(Instant.now());
        passwordResetOtpRepository.save(otp);
        return Map.of("message", "OTP verified");
    }

    public Map<String, Object> resetPassword(ResetPasswordRequest request) {
        validatePassword(request.getNewPassword());
        PasswordResetOtp otp = resolveOtp(request.getEmail(), request.getOtp());
        otp.setUsedAt(Instant.now());
        passwordResetOtpRepository.save(otp);
        User user = otp.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return Map.of("message", "Password updated");
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

    private PasswordResetOtp resolveOtp(String email, String otpValue) {
        if (email == null || email.isBlank()) throw new IllegalArgumentException("Email is required");
        if (otpValue == null || otpValue.isBlank()) throw new IllegalArgumentException("OTP is required");
        User user = userRepository.findByEmail(email.trim().toLowerCase())
            .orElseThrow(() -> new IllegalArgumentException("Invalid OTP"));
        PasswordResetOtp otp = passwordResetOtpRepository.findTopByUserOrderByCreatedAtDesc(user)
            .orElseThrow(() -> new IllegalArgumentException("Invalid OTP"));
        if (otp.getUsedAt() != null) throw new IllegalArgumentException("OTP already used");
        if (otp.getExpiresAt() != null && Instant.now().isAfter(otp.getExpiresAt())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (!otp.getOtp().equals(otpValue.trim())) {
            otp.setAttempts(otp.getAttempts() + 1);
            passwordResetOtpRepository.save(otp);
            throw new IllegalArgumentException("Invalid OTP");
        }
        return otp;
    }

    private String generateOtp() {
        int otp = 100000 + (int) (Math.random() * 900000);
        return String.valueOf(otp);
    }

    private void validatePassword(String password) {
        if (password == null || !PASSWORD_RULE.matcher(password).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters with 1 uppercase, 1 number, and 1 special character");
        }
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

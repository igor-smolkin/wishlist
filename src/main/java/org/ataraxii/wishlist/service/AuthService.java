package org.ataraxii.wishlist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ataraxii.wishlist.database.entity.Session;
import org.ataraxii.wishlist.database.entity.User;
import org.ataraxii.wishlist.database.repository.SessionRepository;
import org.ataraxii.wishlist.database.repository.UserRepository;
import org.ataraxii.wishlist.dto.authentication.AuthRequestDto;
import org.ataraxii.wishlist.dto.authentication.AuthResponseDto;
import org.ataraxii.wishlist.dto.authentication.SessionDto;
import org.ataraxii.wishlist.dto.authentication.UserDto;
import org.ataraxii.wishlist.exception.UnauthorizedException;
import org.ataraxii.wishlist.exception.ConflictException;
import org.ataraxii.wishlist.exception.NotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthResponseDto registerUser(AuthRequestDto req) {
        log.info("Попытка регистрации: username={}", req.getUsername());
        if (userRepository.existsByUsername(req.getUsername())) {
            log.warn("Ошибка регистрации: пользователь с именем '{}' уже существует", req.getUsername());
            throw new ConflictException("Пользователь с таким именем уже существует");
        }

        String hashedPassword = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .username(req.getUsername())
                .password(hashedPassword)
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
//        Присвоение сессии сразу после регистрации (убрать/добавить по желанию)
//        return getAuthResponseDto(user);
        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();

        log.info("Пользователь '{}' успешно зарегистрирован", req.getUsername());
        return AuthResponseDto.builder()
                .user(userDto)
                .build();
    }

    public AuthResponseDto loginUser(AuthRequestDto req) {
        log.info("Попытка входа: username={}", req.getUsername());

        User user = userRepository.findByUsername(req.getUsername())
                .orElse(null);

        if (user == null) {
            log.warn("Ошибка входа: пользователь '{}' не найден", req.getUsername());
            throw new NotFoundException("Пользователь с таким именем не найден");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            log.warn("Ошибка входа: неверный пароль для пользователя '{}'", req.getUsername());
            throw new UnauthorizedException("Неверный пароль");
        }

        log.info("Пользователь вошел в систему: username={}", req.getUsername());

        return getAuthResponseDto(user);
    }

    private AuthResponseDto getAuthResponseDto(User user) {
        UUID sessionId = UUID.randomUUID();

        Session session = Session.builder()
                .sessionId(sessionId)
                .user(user)
                .createdAt(Instant.now())
                .expiredAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        sessionRepository.save(session);

        UserDto userDto = UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .build();

        SessionDto sessionDto = SessionDto.builder()
                .sessionId(session.getSessionId())
                .createdAt(session.getCreatedAt())
                .expiredAt(session.getExpiredAt())
                .build();

        return AuthResponseDto.builder()
                .user(userDto)
                .session(sessionDto)
                .build();
    }
}

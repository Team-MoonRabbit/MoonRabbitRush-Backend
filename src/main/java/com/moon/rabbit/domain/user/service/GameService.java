package com.moon.rabbit.domain.user.service;

import com.moon.rabbit.domain.user.dto.RankResponse;
import com.moon.rabbit.domain.user.dto.UserResponse;
import com.moon.rabbit.global.exception.HttpException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.moon.rabbit.domain.user.entity.User;
import com.moon.rabbit.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class GameService {
    @Value("${game.aes-key}")
    private String aesKey;

    private final UserRepository userRepository;

    @Transactional
    public User updateScore(String encryptedScore, String iv) {
        User user = getCurrentUser();
        int newScore = decryptScore(encryptedScore, iv);
        int currentScore = parseScore(user.getScore());

        if (newScore > currentScore) {
            user.setScore(String.valueOf(newScore));
            userRepository.save(user);
        }
        return user;
    }

    private int decryptScore(String encrypted, String iv) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(encrypted);
            byte[] keyBytes = Base64.getDecoder().decode(aesKey);
            byte[] ivBytes = Base64.getDecoder().decode(iv);

            if (ivBytes.length != 16) throw new RuntimeException("IV 길이 오류: 16바이트여야 합니다.");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new IvParameterSpec(ivBytes));

            String decrypted = new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8).trim();

            if (decrypted.isEmpty()) throw new RuntimeException("복호화 결과가 비어 있습니다.");

            return Integer.parseInt(decrypted);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("복호화된 값이 숫자가 아닙니다.", nfe);
        } catch (Exception e) {
            throw new RuntimeException("점수 복호화 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public List<RankResponse> getRanking() {
        List<User> sortedUsers = userRepository.findAll().stream()
                .sorted((u1, u2) -> Integer.compare(
                        parseScore(u2.getScore()),
                        parseScore(u1.getScore())
                ))
                .collect(Collectors.toList());

        return IntStream.range(0, sortedUsers.size())
                .mapToObj(i -> {
                    User u = sortedUsers.get(i);
                    return new RankResponse(
                            i + 1,
                            u.getEmail(),
                            parseScore(u.getScore())
                    );
                })
                .collect(Collectors.toList());
    }

    private int parseScore(String score) {
        if (score == null || score.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(score);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Transactional
    public UserResponse userInfo() {
        User user = getCurrentUser();

        return new UserResponse(
                user.getEmail(),
                user.getScore()
        );
    }

    @Transactional
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
    }
}

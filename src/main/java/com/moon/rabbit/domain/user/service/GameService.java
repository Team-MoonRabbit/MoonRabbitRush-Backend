package com.moon.rabbit.domain.user.service;

import com.moon.rabbit.domain.user.dto.RankResponse;
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
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    public User updateScore(String encryptedScore) {
        User user = getCurrentUser();
        int newScore = decryptScore(encryptedScore);
        int currentScore = parseScore(user.getScore());
        if (newScore > currentScore) {
            user.setScore(String.valueOf(newScore));
            userRepository.save(user);
        }
        return user;
    }

    private int decryptScore(String encrypted) {
        try {
            byte[] decodedCipher = Base64.getDecoder().decode(encrypted);
            byte[] keyBytes = Base64.getDecoder().decode(aesKey);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);

            byte[] decryptedBytes = cipher.doFinal(decodedCipher);
            String decrypted = new String(decryptedBytes, StandardCharsets.UTF_8).trim();

            return Integer.parseInt(decrypted);
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
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."));
    }
}

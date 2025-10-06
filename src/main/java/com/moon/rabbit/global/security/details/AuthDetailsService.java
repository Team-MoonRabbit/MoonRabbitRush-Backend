package com.moon.rabbit.global.security.details;
import com.moon.rabbit.domain.user.repository.UserRepository;
import com.moon.rabbit.global.security.jwt.dto.UserCredential;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
public class AuthDetailsService implements UserDetailsService {

    private final UserRepository userRepository;


    @Override
    public UserDetails loadUserByUsername(String username) {
        UserCredential credential = userRepository.findCredentialById(username)
                .orElseThrow(() -> new UsernameNotFoundException("유저를 찾을 수 없습니다" + username));

        return new AuthDetails(credential);
    }
}
package org.example.expert.domain.auth.service;

import org.example.expert.config.JwtUtil;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.exception.AuthException;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    public void 회원가입시_이메일을_입력하지_않은_경우() {
        // given
        SignupRequest signupRequest = new SignupRequest(null, "testPassword", "testUser");

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(signupRequest));

        // then
        assertEquals("이메일을 입력하지 않았습니다.", exception.getMessage());
    }


    @Test
    public void 회원가입시_이메일이_중복되는_경우() {
        // given
        SignupRequest signupRequest = new SignupRequest("test@email.com", "testPassword", "USER");

        given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(true);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signup(signupRequest));

        // then
        assertEquals("이미 존재하는 이메일입니다.", exception.getMessage());
    }

    @Test
    public void 회원가입_성공() {
        // given
        SignupRequest signupRequest = new SignupRequest("test@email.com", "testPassword", "USER");
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        UserRole userRole = UserRole.USER;
        User savedUser = new User(signupRequest.getEmail(), encodedPassword, userRole);
        String bearerToken = "bearerToken";

        given(passwordEncoder.encode(signupRequest.getPassword())).willReturn(encodedPassword);
        given(userRepository.existsByEmail(signupRequest.getEmail())).willReturn(false);
        given(userRepository.save(any(User.class))).willReturn(savedUser);
        given(jwtUtil.createToken(savedUser.getId(), savedUser.getEmail(), userRole)).willReturn(bearerToken);

        // when
        SignupResponse response = authService.signup(signupRequest);

        // then
        assertEquals(bearerToken, response.getBearerToken());
        verify(passwordEncoder, times(2)).encode(signupRequest.getPassword());
        verify(userRepository).existsByEmail(signupRequest.getEmail());
        verify(userRepository).save(any(User.class));
        verify(jwtUtil).createToken(savedUser.getId(), savedUser.getEmail(), userRole);
    }

    @Test
    public void 로그인시_이메일_찾을수_없는경우() {
        // given
        SigninRequest signinRequest = new SigninRequest("test@email.com", "testPassword");

        given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> authService.signin(signinRequest));

        // then
        assertEquals("가입되지 않은 유저입니다.", exception.getMessage());
    }

    @Test
    public void 로그인시_비밀번호_불일치() {
        // given
        SigninRequest signinRequest = new SigninRequest("test@email.com", "testPassword");
        User user = new User(signinRequest.getEmail(), signinRequest.getPassword(), UserRole.USER);

        given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())).willReturn(false);

        // when
        AuthException exception = assertThrows(AuthException.class,
                () -> authService.signin(signinRequest));

        // then
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }

    @Test
    public void 로그인_성공() {
        // given
        SigninRequest signinRequest = new SigninRequest("test@email.com", "testPassword");
        User user = new User(signinRequest.getEmail(), signinRequest.getPassword(), UserRole.USER);
        String bearerToken = "bearerToken";

        given(userRepository.findByEmail(signinRequest.getEmail())).willReturn(Optional.of(user));
        given(passwordEncoder.matches(signinRequest.getPassword(), user.getPassword())).willReturn(true);
        given(jwtUtil.createToken(user.getId(), user.getEmail(), UserRole.USER)).willReturn(bearerToken);

        // when
        SigninResponse response = authService.signin(signinRequest);

        // then
        assertNotNull(response);
        assertEquals(bearerToken, response.getBearerToken());
    }
}

package org.example.expert.domain.user.service;

import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    public void 유저_조회_성공() {
        // given
        long userId = 1;

        User user = new User("test@email.com", "testPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        UserResponse response = userService.getUser(userId);

        // then
        verify(userRepository).findById(userId);
        assertEquals("test@email.com", response.getEmail());
        assertEquals(1, response.getId());
        assertEquals("test@email.com", response.getEmail());
    }

    @Test
    public void 유저가_존재하지_않을때() {
        // given
        long userId = 1;

        given(userRepository.findById(userId)).willReturn(Optional.empty());
        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.getUser(userId));

        // then
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    public void 비밀번호_변경_성공() {
        // given
        long userId = 1;
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword123");
        User user = new User("test@email.com", "encodedOldPassword", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("oldPassword", user.getPassword())).willReturn(true);
        given(passwordEncoder.matches("newPassword123", user.getPassword())).willReturn(false);
        given(passwordEncoder.encode("newPassword123")).willReturn("encodedNewPassword");

        // when
        userService.changePassword(userId, request);

        //then
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).findById(userId);
    }

    @Test
    public void 새로운_비밀번호와_기존_비밀번호가_같은경우_예외처리() {
        // given
        long userId = 1;
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword123");
        User user = new User("test@email.com", "encodedOldPassword", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(passwordEncoder.matches("newPassword123", user.getPassword())).willReturn(true);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        // then
        assertEquals("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.", exception.getMessage());
    }

    @Test
    public void 현재_비밀번호를_잘못입력한_경우_예외처리() {
        // given
        long userId = 1;
        UserChangePasswordRequest request = new UserChangePasswordRequest("wrongOldPassword", "newPassword123");
        User user = new User("test@email.com", "encodedOldPassword", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        //PotentialStubbingProblem: Strict stubbing argument mismatch
        lenient().when(passwordEncoder.matches("wrongOldPassword", user.getPassword())).thenReturn(false);

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userService.changePassword(userId, request));

        // then
        assertEquals("잘못된 비밀번호입니다.", exception.getMessage());
    }

    @Test
    public void 새_비밀번호_유효성_검사_성공() {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword123");

        // when, then
        assertDoesNotThrow(() -> userService.validatePassword(request));
    }

    @Test
    public void 새_비밀번호_유효성_검사_실패() {
        // given
        UserChangePasswordRequest request = new UserChangePasswordRequest("oldPassword", "newPassword");

        // When & Then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            userService.validatePassword(request)
        );

        assertEquals("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.", exception.getMessage());
    }
}

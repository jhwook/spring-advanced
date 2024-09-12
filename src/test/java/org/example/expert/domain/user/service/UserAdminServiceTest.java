package org.example.expert.domain.user.service;

import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserRoleChangeRequest;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
public class UserAdminServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserAdminService userAdminService;

    @Test
    public void 유저_권한_변경_성공() {
        // given
        long userId = 1;
        UserRoleChangeRequest userRoleChangeRequest = new UserRoleChangeRequest("ADMIN");

        User user = new User("test@email.com", "testPassword", UserRole.USER);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        // when
        userAdminService.changeUserRole(userId, userRoleChangeRequest);

        // then
        assertEquals(UserRole.ADMIN, user.getUserRole());
    }

    @Test
    public void 유저가_존재하지_않을때_예외처리() {
        // given
        long userId = 1;
        UserRoleChangeRequest userRoleChangeRequest = new UserRoleChangeRequest("ADMIN");

        given( userRepository.findById(userId)).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> userAdminService.changeUserRole(userId, userRoleChangeRequest));

        // then
        assertEquals("User not found", exception.getMessage());
    }
}

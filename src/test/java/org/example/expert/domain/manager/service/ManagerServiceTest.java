package org.example.expert.domain.manager.service;

import io.jsonwebtoken.Claims;
import org.example.expert.config.JwtUtil;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.manager.dto.request.ManagerSaveRequest;
import org.example.expert.domain.manager.dto.response.ManagerResponse;
import org.example.expert.domain.manager.dto.response.ManagerSaveResponse;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.manager.repository.ManagerRepository;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.example.expert.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ManagerServiceTest {

    @Mock
    private ManagerRepository managerRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private JwtUtil jwtUtil;
    @InjectMocks
    private ManagerService managerService;

    @Test
    public void manager_목록_조회_시_Todo가_없다면_NPE_에러를_던진다() {
        // given
        long todoId = 1L;
        given(todoRepository.findById(todoId)).willReturn(Optional.empty());

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> managerService.getManagers(todoId));
        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    void todo의_user가_null인_경우_예외가_발생한다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        long todoId = 1L;
        long managerUserId = 2L;

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "user", null);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when & then
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () ->
            managerService.saveManager(authUser, todoId, managerSaveRequest)
        );

        assertEquals("담당자를 등록하려고 하는 유저가 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
    }

    @Test // 테스트코드 샘플
    public void manager_목록_조회에_성공한다() {
        // given
        long todoId = 1L;
        User user = new User("user1@example.com", "password", UserRole.USER);
        Todo todo = new Todo("Title", "Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        Manager mockManager = new Manager(todo.getUser(), todo);
        List<Manager> managerList = List.of(mockManager);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findByTodoIdWithUser(todoId)).willReturn(managerList);

        // when
        List<ManagerResponse> managerResponses = managerService.getManagers(todoId);

        // then
        assertEquals(1, managerResponses.size());
        assertEquals(mockManager.getId(), managerResponses.get(0).getId());
        assertEquals(mockManager.getUser().getEmail(), managerResponses.get(0).getUser().getEmail());
    }

    @Test // 테스트코드 샘플
    void todo가_정상적으로_등록된다() {
        // given
        AuthUser authUser = new AuthUser(1L, "a@a.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);  // 일정을 만든 유저

        long todoId = 1L;
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);

        long managerUserId = 2L;
        User managerUser = new User("b@b.com", "password", UserRole.USER);  // 매니저로 등록할 유저
        ReflectionTestUtils.setField(managerUser, "id", managerUserId);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId); // request dto 생성

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(userRepository.findById(managerUserId)).willReturn(Optional.of(managerUser));
        given(managerRepository.save(any(Manager.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ManagerSaveResponse response = managerService.saveManager(authUser, todoId, managerSaveRequest);

        // then
        assertNotNull(response);
        assertEquals(managerUser.getId(), response.getUser().getId());
        assertEquals(managerUser.getEmail(), response.getUser().getEmail());
    }

    @Test
    public void 일정_작성자는_본인을_담당자로_등록할_수_없다() {
        // given
        AuthUser authUser = new AuthUser(1L, "test@email.com", UserRole.USER);
        long todoId = 1;
        long managerUserId = 1;

        User user = User.fromAuthUser(authUser);
        Todo todo = new Todo("Test Title", "Test Contents", "Sunny", user);
        ReflectionTestUtils.setField(todo, "user", user);

        ManagerSaveRequest managerSaveRequest = new ManagerSaveRequest(managerUserId);

        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(userRepository.findById(managerUserId)).willReturn(Optional.of(user));

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.saveManager(authUser, todoId, managerSaveRequest));

        // then
        assertEquals("일정 작성자는 본인을 담당자로 등록할 수 없습니다.", exception.getMessage());
    }

    @Test
    public void 매니저를_성공적으로_삭제() {
        // given
        String bearerToken = "bearerToken";
        long userId = 1;
        long todoId = 1;
        long managerId = 1;

        User user = new User("test@email.com", "testPassword", UserRole.USER);
        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", todoId);
        ReflectionTestUtils.setField(todo, "user", user);

        Manager manager = new Manager();
        ReflectionTestUtils.setField(manager, "id", managerId);
        ReflectionTestUtils.setField(manager, "todo", todo);

        Claims claims = Mockito.mock(Claims.class);
        given(jwtUtil.extractClaims(bearerToken.substring(7))).willReturn(claims);
        given(claims.getSubject()).willReturn(String.valueOf(userId));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

        // when
        managerService.deleteManager(bearerToken, todoId, managerId);

        // then
        verify(managerRepository).delete(manager);
    }

    @Test
    public void 일정작성자가_아닌경우_예외발생() {
        // given
        String bearerToken = "bearerToken";
        long userId = 1;
        long anotherUserId = 2;
        long todoId = 1;
        long managerId = 1;

        User user = new User("test@email.com", "testPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);
        User anotherUser = new User("test2@email.com", "testPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", anotherUserId);
        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", todoId);
        ReflectionTestUtils.setField(todo, "user", anotherUser);

        Claims claims = Mockito.mock(Claims.class);
        given(jwtUtil.extractClaims(bearerToken.substring(7))).willReturn(claims);
        given(claims.getSubject()).willReturn(String.valueOf(userId));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(bearerToken, todoId, managerId));

        // then
        assertEquals("해당 일정을 만든 유저가 유효하지 않습니다.", exception.getMessage());
    }

    @Test
    public void 해당일정의_담당자가_아닌경우_예외발생() {
        // given
        String bearerToken = "bearerToken";
        long userId = 1;
        long todoId = 1;
        long anotherTodoId = 2;
        long managerId = 1;

        User user = new User("test@email.com", "testPassword", UserRole.USER);
        ReflectionTestUtils.setField(user, "id", userId);

        Todo todo = new Todo();
        ReflectionTestUtils.setField(todo, "id", todoId);
        ReflectionTestUtils.setField(todo, "user", user);

        Todo anotherTodo = new Todo();
        ReflectionTestUtils.setField(todo, "id", anotherTodoId);
        ReflectionTestUtils.setField(todo, "user", user);

        Manager manager = new Manager(user, anotherTodo);
        ReflectionTestUtils.setField(manager, "id", managerId);

        Claims claims = Mockito.mock(Claims.class);
        given(jwtUtil.extractClaims(bearerToken.substring(7))).willReturn(claims);
        given(claims.getSubject()).willReturn(String.valueOf(userId));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(todoRepository.findById(todoId)).willReturn(Optional.of(todo));
        given(managerRepository.findById(managerId)).willReturn(Optional.of(manager));

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> managerService.deleteManager(bearerToken, todoId, managerId));

        // then
        assertEquals("해당 일정에 등록된 담당자가 아닙니다.", exception.getMessage());
    }
}

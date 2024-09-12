package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;

    @Mock
    private WeatherClient weatherClient;

    @InjectMocks
    private TodoService todoService;

    @Test
    public void todo_저장_성공() {
        // given
        AuthUser authUser = new AuthUser(1L, "test@email.com", UserRole.USER);
        TodoSaveRequest todoSaveRequest = new TodoSaveRequest("title", "content");

        User user = User.fromAuthUser(authUser);

        String weather = "sunny";

        Todo todo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        ReflectionTestUtils.setField(todo, "id", 1L);

        given(weatherClient.getTodayWeather()).willReturn(weather);
        given(todoRepository.save(any(Todo.class))).willReturn(todo);

        // when
        TodoSaveResponse response = todoService.saveTodo(authUser, todoSaveRequest);

        // then
        assertNotNull(response);
        assertEquals(todo.getId(), response.getId());
        assertEquals(todo.getTitle(), response.getTitle());
        assertEquals(todo.getContents(), response.getContents());
        assertEquals(weather, response.getWeather());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(user.getEmail(), response.getUser().getEmail());
    }

    @Test
    public void todo_목록_조회_성공() {
        // given
        int page = 1;
        int size = 10;

        Pageable pageable = PageRequest.of(page - 1, size);

        User user = new User("test@email.com", "testPassword", UserRole.USER);

        Todo todo1 = new Todo("title1", "content1", "sunny", user);
        Todo todo2 = new Todo("title2", "content2", "rainy", user);

        Page<Todo> todos = new PageImpl<>(List.of(todo1, todo2));

        given(todoRepository.findAllByOrderByModifiedAtDesc(pageable)).willReturn(todos);

        // when
        Page<TodoResponse> result = todoService.getTodos(page, size);

        // then
        assertEquals(2, result.getSize());

        assertEquals("title1", result.getContent().get(0).getTitle());
        assertEquals("sunny", result.getContent().get(0).getWeather());
        assertEquals("test@email.com", result.getContent().get(0).getUser().getEmail());

        assertEquals("title2", result.getContent().get(1).getTitle());
        assertEquals("rainy", result.getContent().get(1).getWeather());
        assertEquals("test@email.com", result.getContent().get(1).getUser().getEmail());
    }

    @Test
    public void todo_단건_조회_성공() {
        // given
        long todoId = 1;

        User user = new User("test@email.com", "testPassword", UserRole.USER);
        Todo todo = new Todo("title", "content", "sunny", user);
        ReflectionTestUtils.setField(todo, "id", todoId);

        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.of(todo));

        // when
        TodoResponse response = todoService.getTodo(todoId);

        // then
        assertEquals(todoId, response.getId());
        assertEquals("title", response.getTitle());
        assertEquals("content", response.getContents());
        assertEquals("sunny", response.getWeather());
        assertEquals("test@email.com", response.getUser().getEmail());
    }

    @Test
    public void todo가_존재하지_않을때_예외처리() {
        // given
        long todoId = 1;
        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class,
                () -> todoService.getTodo(todoId));

        // then
        assertEquals("Todo not found", exception.getMessage());
    }
}

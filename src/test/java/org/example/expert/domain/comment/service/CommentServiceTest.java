package org.example.expert.domain.comment.service;

import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.comment.repository.CommentRepository;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.common.exception.ServerException;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TodoRepository todoRepository;
    @InjectMocks
    private CommentService commentService;

    @Test
    public void comment_등록_중_할일을_찾지_못해_에러가_발생한다() {
        // given
        long todoId = 1;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);

        given(todoRepository.findById(anyLong())).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            commentService.saveComment(authUser, todoId, request);
        });

        // then
        assertEquals("Todo not found", exception.getMessage());
    }

    @Test
    public void comment를_정상적으로_등록한다() {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        Todo todo = mock(Todo.class);

        List<Manager> managers = new ArrayList<>();
        Manager manager = new Manager(user, todo);
        managers.add(manager);

        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

        given(todo.getManagers()).willReturn(managers);
//        doReturn(true).when(todo.getManagers()).contains(any(Manager.class));

        Comment comment = new Comment(request.getContents(), user, todo);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);

        // when
        CommentSaveResponse result = commentService.saveComment(authUser, todoId, request);

        // then
        assertNotNull(result);
    }

    @Test
    public void todo의_매니저가_아니라면_댓글을_등록하지_못함() {
        // given
        long todoId = 1L;
        CommentSaveRequest request = new CommentSaveRequest("contents");
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        Todo todo = mock(Todo.class);
        List<Manager> managers = new ArrayList<>();
        Manager manager = new Manager(user, todo);
        managers.add(manager);

        given(todoRepository.findById(anyLong())).willReturn(Optional.of(todo));

        given(todo.getManagers()).willReturn(managers);

        Comment comment = new Comment(request.getContents(), user, todo);
        given(commentRepository.save(any(Comment.class))).willReturn(comment);
    }

    @Test
    public void comment_리스트를_성공적으로_조회() {
        // given
        long todoId = 1;
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        AuthUser authUser1 = new AuthUser(1L, "email1", UserRole.USER);
        AuthUser authUser2 = new AuthUser(2L, "email2", UserRole.USER);
        User user1 = User.fromAuthUser(authUser1);
        User user2 = User.fromAuthUser(authUser2);

        Todo todo = new Todo("title", "title", "contents", user);

        Comment comment1 = new Comment("content1", user1, todo);
        Comment comment2 = new Comment("content2", user2, todo);

        List<Comment> commentList = List.of(comment1, comment2);
        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(commentList);

        // when
        List<CommentResponse> dtoList = commentService.getComments(todoId);

        // then
        assertEquals(2, dtoList.size());

        assertEquals(comment1.getId(), dtoList.get(0).getId());
        assertEquals(comment1.getContents(), dtoList.get(0).getContents());
        assertEquals(comment1.getUser().getId(), dtoList.get(0).getUser().getId());
        assertEquals(comment1.getUser().getEmail(), dtoList.get(0).getUser().getEmail());

        assertEquals(comment2.getId(), dtoList.get(1).getId());
        assertEquals(comment2.getContents(), dtoList.get(1).getContents());
        assertEquals(comment2.getUser().getId(), dtoList.get(1).getUser().getId());
        assertEquals(comment2.getUser().getEmail(), dtoList.get(1).getUser().getEmail());
    }

    @Test
    public void comment_리스트에_댓글_없음() {
        // given
        long todoId = 1;
        given(commentRepository.findByTodoIdWithUser(todoId)).willReturn(new ArrayList<>());

        // when
        List<CommentResponse> dtoList = commentService.getComments(todoId);

        // then
        assertTrue(dtoList.isEmpty());
    }
}

package org.example.expert.domain.comment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.config.AuthUserArgumentResolver;
import org.example.expert.config.WebConfig;
import org.example.expert.domain.comment.dto.request.CommentSaveRequest;
import org.example.expert.domain.comment.dto.response.CommentResponse;
import org.example.expert.domain.comment.dto.response.CommentSaveResponse;
import org.example.expert.domain.comment.service.CommentService;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = CommentController.class,
        excludeFilters = {
                @ComponentScan.Filter(
                        type = FilterType.ASSIGNABLE_TYPE,
                        classes = WebConfig.class
                )
        }
)
public class CommentControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private AuthUserArgumentResolver authUserArgumentResolver;

    @Test
    public void 댓글_저장_성공() throws Exception {
        // given
        long todoId = 1L;
        AuthUser authUser = new AuthUser(1L, "test@email.com", UserRole.USER);
        UserResponse userResponse = new UserResponse(1L, "test@email.com");
        CommentSaveRequest commentSaveRequest = new CommentSaveRequest("contents");
        CommentSaveResponse commentSaveResponse = new CommentSaveResponse(1L, "contents", userResponse);

        given(authUserArgumentResolver.supportsParameter(any())).willReturn(true);
        given(authUserArgumentResolver.resolveArgument(any(), any(), any(), any())).willReturn(authUser);
        given(commentService.saveComment(any(AuthUser.class), anyLong(), any(CommentSaveRequest.class))).willReturn(commentSaveResponse);
        // when
        ResultActions resultActions = mockMvc.perform(post("/todos/{todoId}/comments", todoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(commentSaveRequest))
                .header("Authorization", "Bearer Token")
        );

        // then
        resultActions.andExpect(status().isOk());
    }

    @Test
    public void 댓글_목록_조회_성공() throws Exception {
        // given
        long todoId = 1L;
        CommentResponse commentResponse1 = new CommentResponse(1L, "First comment", new UserResponse(1L, "user1@example.com"));
        CommentResponse commentResponse2 = new CommentResponse(2L, "Second comment", new UserResponse(2L, "user2@example.com"));
        List<CommentResponse> commentResponses = Arrays.asList(commentResponse1, commentResponse2);

        given(commentService.getComments(anyLong())).willReturn(commentResponses);

        // when
        ResultActions resultActions = mockMvc.perform(get("/todos/{todoId}/comments", todoId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer someToken")
        );

        // then
        resultActions.andExpect(status().isOk());
    }
}

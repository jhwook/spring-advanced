package org.example.expert.domain.auth.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expert.domain.auth.dto.request.SigninRequest;
import org.example.expert.domain.auth.dto.request.SignupRequest;
import org.example.expert.domain.auth.dto.response.SigninResponse;
import org.example.expert.domain.auth.dto.response.SignupResponse;
import org.example.expert.domain.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void signup_성공() throws Exception {
        // given
        SignupRequest signupRequest = new SignupRequest("test@email.com", "testPassword123", "USER");
        SignupResponse signupResponse = new SignupResponse("bearerToken");

        given(authService.signup(any(SignupRequest.class))).willReturn(signupResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest)));

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.bearerToken").value("bearerToken"));
    }

    @Test
    public void signup_실패() throws Exception {
        // given
        SignupRequest signupRequest = new SignupRequest("", "", "");

        // when
        ResultActions resultActions = mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signupRequest))
        );

        // then
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    public void signin_성공() throws Exception {
        // given
        SigninRequest signinRequest = new SigninRequest("test@email.com", "testPassword123");
        SigninResponse signinResponse = new SigninResponse("bearerToken");

        given(authService.signin(any(SigninRequest.class))).willReturn(signinResponse);

        // when
        ResultActions resultActions = mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signinRequest))
        );

        // then
        resultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.bearerToken").value("bearerToken"));
    }

    @Test
    public void signin_실패() throws Exception {
        // given
        SigninRequest signinRequest = new SigninRequest("", "");

        // when
        ResultActions resultActions = mockMvc.perform(post("/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signinRequest))
        );

        // then
        resultActions.andExpect(status().isBadRequest());
    }
}

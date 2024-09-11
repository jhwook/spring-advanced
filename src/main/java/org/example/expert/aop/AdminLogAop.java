package org.example.expert.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j(topic = "AdminLogAop")
@Aspect
@Component
public class AdminLogAop {
    @Pointcut("execution(* org.example.expert.domain.user.controller.UserAdminController.*(..))")
    private void userAdmin() {
    }

    @Pointcut("execution(* org.example.expert.domain.comment.controller.CommentAdminController.*(..))")
    private void commentAdmin() {
    }

    @Before("userAdmin() || commentAdmin()")
    public void getInfoBeforeApiRequest(JoinPoint joinPoint) {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes.getRequest();

        // 요청한 사용자의 ID
        Long userId = (Long) request.getAttribute("userId");
        // API 요청 URL
        String apiRequestUrl = String.valueOf(request.getRequestURL());
        // API 요청 시간
        LocalDateTime apiRequestTime = LocalDateTime.now();

        log.info("요청 사용자의 ID: " + userId + " / API 요청 시간: " + apiRequestTime + " / API 요청 URL: " + apiRequestUrl);
    }
}

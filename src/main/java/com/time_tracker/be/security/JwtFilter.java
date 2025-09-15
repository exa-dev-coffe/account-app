package com.time_tracker.be.security;

import com.time_tracker.be.annotation.RequireAuth;
import com.time_tracker.be.exception.NotAuthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class JwtFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletRequest request;

    public JwtFilter(JwtTokenProvider jwtTokenProvider, HttpServletRequest request) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.request = request;
    }

    @Around("@annotation(requireAuth)")
    public Object checkAuth(ProceedingJoinPoint pjp, RequireAuth requireAuth) throws Throwable {
        String header = request.getHeader("Authorization");
        String token = jwtTokenProvider.resolveToken(header);

        if (token == null || !jwtTokenProvider.validateToken(token)) {
            throw new NotAuthorizedException("Token is not valid");
        }

        return pjp.proceed();
    }
}
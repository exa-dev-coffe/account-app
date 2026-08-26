package com.account_service.be.resolver;

import com.account_service.be.annotation.CurrentUser;
import com.account_service.be.lib.JwtService;
import com.account_service.be.utils.commons.CurrentUserDto;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Slf4j
@Component
public class CurrentUserResolver implements HandlerMethodArgumentResolver {
    private final JwtService jwtService;

    public CurrentUserResolver(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) &&
                parameter.getParameterType().equals(CurrentUserDto.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = (HttpServletRequest) webRequest.getNativeRequest();

        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            Claims claims = jwtService.getClaims(token);
            CurrentUserDto account = new CurrentUserDto();
            account.setUserId(claims.get("userId", Integer.class));
            account.setEmail(claims.get("email", String.class));
            account.setFullName(claims.get("fullName", String.class));
            account.setRole(claims.get("role", String.class));
            account.setRoleId(claims.get("roleId", Integer.class));
            account.setPhoto(claims.get("photo", String.class));
            account.setToken(token);
            return account;
        } else {
            return null;
        }
    }
}

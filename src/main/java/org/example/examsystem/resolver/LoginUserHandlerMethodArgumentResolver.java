package org.example.examsystem.resolver;

import io.jsonwebtoken.Claims;
import org.example.examsystem.annotation.LoginUser;
import org.example.examsystem.utils.JwtUtils;
import org.example.examsystem.vo.Result;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // 判断参数是否有 @LoginUser 注解，且类型是 Long
        return parameter.hasParameterAnnotation(LoginUser.class) &&
                parameter.getParameterType().isAssignableFrom(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

        // 从请求头获取 Authorization
        String authorization = webRequest.getHeader("Authorization");

        if (authorization == null || !authorization.startsWith("Bearer ")) {

            throw new RuntimeException("未登录或缺少授权信息");
        }

        String token = authorization.substring(7);

        try {
            Claims claims = JwtUtils.parseJWT(token);
            Object uid = claims.get("userId");
            if (uid == null) {
                throw new RuntimeException("授权信息不完整");
            }

            if (uid instanceof Number) {
                return ((Number) uid).longValue();
            } else {
                return Long.valueOf(uid.toString());
            }
        } catch (Exception e) {
            throw new RuntimeException("token无效或已过期");
        }

    }
}

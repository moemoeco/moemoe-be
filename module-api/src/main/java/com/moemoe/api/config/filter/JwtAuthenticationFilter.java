package com.moemoe.api.config.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moemoe.api.config.handler.ErrorResponseBody;
import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.core.service.jwt.exception.JwtMalformedException;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.UserEntity;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.moemoe.core.service.jwt.JwtService.AUTHENTICATION_HEADER;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader(AUTHENTICATION_HEADER);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 특정 경로 제외
            if (!isDoNotFilteredUri(requestURI)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(ErrorResponseBody.of("EMPTY_AUTH_HEADER", "Authorization header is missing or empty.")));
                log.error("Authorization header is missing or empty.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.split(" ")[1];
        try {
            String email = jwtService.getEmail(token);
            String role = jwtService.getRole(token);
            if (!ObjectUtils.isEmpty(email) && jwtService.isValidToken(token, email)) {
                UserDetails userDetails = UserEntity.builder()
                        .email(email)
                        .role(UserRole.valueOf(role))
                        .build();

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (JwtMalformedException exception) {
            log.error("Malformed JWT Token: {}", exception.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(ErrorResponseBody.of("INVALID_AUTH_HEADER", "The provided token is invalid or malformed.")));
            return;
        } catch (JwtExpiredException exception) {
            log.info("Expired access token: {}", exception.getMessage());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(ErrorResponseBody.of("EXPIRED_AUTH_HEADER", "The access token has expired. Please refresh your token.")));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDoNotFilteredUri(String requestURI) {
        return requestURI.startsWith("/swagger-ui")
                || requestURI.startsWith("/api-docs")
                || requestURI.startsWith("/oauth")
                || requestURI.equals("/users/refresh")
                || requestURI.equals("/users/logout");
    }
}



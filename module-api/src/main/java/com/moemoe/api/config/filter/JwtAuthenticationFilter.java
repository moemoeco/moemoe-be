package com.moemoe.api.config.filter;

import com.moemoe.core.service.jwt.JwtService;
import com.moemoe.core.service.jwt.exception.JwtExpiredException;
import com.moemoe.core.service.jwt.exception.JwtMalformedException;
import com.moemoe.mongo.constant.UserRole;
import com.moemoe.mongo.entity.User;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader(AUTHENTICATION_HEADER);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // 특정 경로 제외
            if (!isDoNotFilteredUri(requestURI)) {
                log.error("Authentication header is invalid.");
            }
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.split(" ")[1];
        try {
            String email = jwtService.getEmail(token);
            String role = jwtService.getRole(token);
            if (!ObjectUtils.isEmpty(email) && jwtService.isValidToken(token, email)) {
                UserDetails userDetails = User.builder()
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
            return;
        } catch (JwtExpiredException exception) {
            log.info("Expired access token! Trying to reissue new access token using refresh token.");
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDoNotFilteredUri(String requestURI) {
        return requestURI.startsWith("/swagger-ui")
                || requestURI.startsWith("/api-docs")
                || requestURI.startsWith("/oauth");
    }
}



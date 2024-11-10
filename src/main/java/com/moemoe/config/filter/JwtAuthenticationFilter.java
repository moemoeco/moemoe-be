package com.moemoe.config.filter;

import com.moemoe.domain.User;
import com.moemoe.domain.UserRole;
import com.moemoe.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.MalformedJwtException;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTHENTICATION_HEADER = "Authorization";
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHENTICATION_HEADER);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("Authentication header is invalid.");
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
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (MalformedJwtException exception) {
            log.error(exception.getMessage());
        } catch (ExpiredJwtException exception) {
            log.info("Expired access token! Trying to reissue new access token using refresh token.");

            // 리프레시 토큰을 Redis에서 조회하여 존재하는지 확인
            // 리프레시 토큰 유효 기간이 살아있는 경우 - (Exception)
            // 리프레시 토큰 유효 기간이 지난 경우 - logout api redirect
        }
        filterChain.doFilter(request, response);
    }
}



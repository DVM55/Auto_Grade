package com.example.Auto_Grade.security;

import com.example.Auto_Grade.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            setErrorResponse(response, "Token không hợp lệ!", 401);
            return;
        }

        String token = authHeader.substring(7);

        try {
            jwtService.validateAccessToken(token);

            Long accountId = jwtService.getAccountIdFromAccessToken(token);

            if(jwtService.isTokenBlacklisted(token)) {
                setErrorResponse(response, "Token bị thu hồi", HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            String role = jwtService.getRoleFromAccessToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            accountId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            setErrorResponse(response, "Token đã hết hạn", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (MalformedJwtException e) {
            setErrorResponse(response, "Token không hợp lệ (Malformed)", HttpServletResponse.SC_BAD_REQUEST);
        } catch (UnsupportedJwtException e) {
            setErrorResponse(response, "Token không được hỗ trợ", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (SignatureException e) {
            setErrorResponse(response, "Chữ ký JWT không hợp lệ", HttpServletResponse.SC_UNAUTHORIZED);
        } catch (Exception e) {
            setErrorResponse(response, "Lỗi xác thực JWT: " + e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        List<String> excludedPaths = List.of(
                "api/v1/auth/**",
                "/ws",
                "/swagger-ui",
                "/v3/api-docs",
                "/swagger-resources",
                "/webjars",
                "/actuator"
        );
        return excludedPaths.stream().anyMatch(path::startsWith);
    }

    private void setErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = String.format("""
        {
            "timestamp": "%s",
            "status": %d,
            "error": "Unauthorized",
            "message": "%s"
        }
        """, java.time.LocalDateTime.now(), status, message);

        response.getWriter().write(json);
    }

}
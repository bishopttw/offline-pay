package com.bishop.offlinepay.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class SessionAuthFilter implements Filter{

    private static final List<String> PUBLIC_PATHS = List.of(
            "/accounts/signup",
            "/accounts/login",
            "/signup",
            "/login",
            "/css",
            "/h2-console",
            "/payments/webhook"
    );

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
        throws IOException,ServletException{
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String path = request.getRequestURI();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);

        if (isPublic){
            chain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("accountId") == null) {
            if (path.startsWith("/accounts") || path.startsWith("/transactions")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"You must be logged in\"}");
            } else {
                response.sendRedirect("/login");
            }
            return;
        }

        chain.doFilter(request, response);
    }
}

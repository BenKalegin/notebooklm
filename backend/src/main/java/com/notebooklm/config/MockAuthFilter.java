package com.notebooklm.config;

import com.notebooklm.model.User;
import com.notebooklm.repository.UserRepository;
import com.notebooklm.util.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class MockAuthFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public MockAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String email = request.getHeader("X-User-Email");
        if (email == null) {
            // Default user for convenience if no header
            email = "demo@example.com";
        }
        
        final String finalEmail = email;
        User user = userRepository.findByEmail(finalEmail).orElseGet(() -> {
            User u = new User(UUID.randomUUID(), finalEmail, Instant.now());
            return userRepository.save(u);
        });
        
        UserContext.setCurrentUser(user);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}

package net.dirtydeeds.discordsoundboard.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRoleConfig userRoleConfig;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract Discord user info
        String userId = oAuth2User.getAttribute("id");
        String username = oAuth2User.getAttribute("username");
        String discriminator = oAuth2User.getAttribute("discriminator");
        String avatar = oAuth2User.getAttribute("avatar");
        String globalName = oAuth2User.getAttribute("global_name");

        // Get user's roles from configuration
        List<String> roles = userRoleConfig.getUserRoles(userId);

        // Get user's permissions from their roles
        Set<String> permissions = userRoleConfig.getUserPermissions(userId);

        // Create claims for JWT
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", username);
        claims.put("discriminator", discriminator);
        claims.put("avatar", avatar);
        claims.put("globalName", globalName);
        claims.put("roles", roles);
        claims.put("permissions", new ArrayList<>(permissions));

        // Generate JWT token
        String token = jwtUtil.generateToken(userId, claims);

        // Redirect to frontend with token
        String redirectUrl = frontendUrl + "?token=" + token;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}

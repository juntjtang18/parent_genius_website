package ca.parentgeniusai.website.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${strapi.root.url}")
    private String strapiRootUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StrapiAuthenticationFilter strapiAuthFilter) throws Exception {
        http
            .csrf().disable()
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository())
            )
            .authorizeHttpRequests(authz -> authz
                // --- START: MODIFIED ACCESS RULES ---
                // Require 'EDITOR' role for admin-specific URLs
                .requestMatchers("/api/v1/admin/**", "/course-list").hasRole("EDITOR")
                // Require authentication for the functions pages
                .requestMatchers("/function-article-list", "/article", "/new-article", "/edit-article").authenticated()
                // Allow all other requests to be accessed publicly
                .anyRequest().permitAll()
                // --- END: MODIFIED ACCESS RULES ---
            )
            .addFilterBefore(strapiAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/signin")
                .loginProcessingUrl("/do-login")
                .successHandler(new SimpleUrlAuthenticationSuccessHandler("/"))
                .failureUrl("/signin?error=true")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            );

        logger.debug("Security filter chain configured with updated access rules.");
        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AuthenticationManager strapiAuthenticationManager(RestTemplate restTemplate) {
        return new StrapiAuthenticationManager(restTemplate, strapiRootUrl);
    }

    @Bean
    public StrapiAuthenticationFilter strapiAuthenticationFilter(
        RestTemplate restTemplate,
        AuthenticationManager strapiAuthenticationManager,
        SecurityContextRepository securityContextRepository) {

        StrapiAuthenticationFilter filter = new StrapiAuthenticationFilter(restTemplate, strapiRootUrl);
        filter.setAuthenticationManager(strapiAuthenticationManager);
        filter.setFilterProcessesUrl("/do-login");
        filter.setSecurityContextRepository(securityContextRepository);
        filter.setAuthenticationSuccessHandler((request, response, authentication) -> response.sendRedirect("/"));
        filter.setAuthenticationFailureHandler((request, response, exception) -> response.sendRedirect("/login?error=true"));

        return filter;
    }
}
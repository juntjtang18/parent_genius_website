package ca.parentgeniusai.website.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${strapi.root.url:http://localhost:8080/}")
    private String strapiRootUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, StrapiAuthenticationFilter strapiAuthFilter) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/signin", "/do-login", "/signup", "/css/**", "/images/**", "/").permitAll()
                .requestMatchers("/protected", "/profile").authenticated()
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/signin")
                .loginProcessingUrl("/do-login")
                .successHandler(new SimpleUrlAuthenticationSuccessHandler("/") {
                    @Override
                    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                            Authentication authentication) throws java.io.IOException, jakarta.servlet.ServletException {
                        logger.info("Login successful for user: " + authentication.getName());
                        logger.debug("Session ID in success handler: " + request.getSession().getId());
                        super.onAuthenticationSuccess(request, response, authentication);
                    }
                })
                .failureHandler(new SimpleUrlAuthenticationFailureHandler("/signin?error=true") {
                    @Override
                    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                            org.springframework.security.core.AuthenticationException exception) throws java.io.IOException, jakarta.servlet.ServletException {
                        logger.warn("Login failed: " + exception.getMessage());
                        super.onAuthenticationFailure(request, response, exception);
                    }
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .addFilterBefore(strapiAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            );

        return http.build();
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
    public StrapiAuthenticationManager strapiAuthenticationManager(RestTemplate restTemplate) {
        return new StrapiAuthenticationManager(restTemplate, strapiRootUrl);
    }

    @Bean
    public StrapiAuthenticationFilter strapiAuthenticationFilter(RestTemplate restTemplate, StrapiAuthenticationManager strapiAuthenticationManager) {
        StrapiAuthenticationFilter filter = new StrapiAuthenticationFilter(restTemplate, strapiAuthenticationManager);
        filter.setAuthenticationSuccessHandler(new SimpleUrlAuthenticationSuccessHandler("/") {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, 
                    Authentication authentication) throws java.io.IOException, jakarta.servlet.ServletException {
                logger.info("Strapi filter login successful for user: " + authentication.getName());
                logger.debug("Session ID in filter success handler: " + request.getSession().getId());
                super.onAuthenticationSuccess(request, response, authentication);
            }
        });
        filter.setAuthenticationFailureHandler(new SimpleUrlAuthenticationFailureHandler("/signin?error=true") {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, 
                    org.springframework.security.core.AuthenticationException exception) throws java.io.IOException, jakarta.servlet.ServletException {
                logger.warn("Strapi authentication failed: " + exception.getMessage());
                super.onAuthenticationFailure(request, response, exception);
            }
        });
        return filter;
    }
}
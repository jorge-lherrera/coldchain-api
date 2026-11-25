package com.coldchain.shared.security;

import com.coldchain.shared.config.web.exception.ProblemErrorResponder;
import com.coldchain.shared.config.web.ratelimit.ClientAddress;
import com.coldchain.shared.config.web.ratelimit.RateLimitFilter;
import com.coldchain.shared.config.web.ratelimit.RateLimitProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

    private static final String PERMISSIONS_POLICY =
            "accelerometer=(), camera=(), geolocation=(), gyroscope=(), microphone=(), payment=(), usb=()";

    private static final long HSTS_MAX_AGE_SECONDS = 31_536_000L;

    private static final List<String> PUBLIC_ROUTES = List.of(
            "/v1/organizations",
            "/v1/auth/login",
            "/v1/auth/refresh",
            "/v1/auth/client-token",
            "/v1/auth/activation");

    private static final List<String> DOCUMENTATION_ROUTES = List.of(
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-ui.html",
            "/swagger-ui/**");

    private static final String INTERNAL_ROUTES = "/internal/**";

    private static final AuthenticationTrustResolver TRUST = new AuthenticationTrustResolverImpl();

    private final String jwtSecret;

    public SecurityConfig(@Value("${coldchain.security.jwt-secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    @Bean
    @Order(1)
    SecurityFilterChain internalFilterChain(HttpSecurity http, ProblemErrorResponder problemErrorResponder)
            throws Exception {
        return statelessResourceServer(http.securityMatcher(INTERNAL_ROUTES), problemErrorResponder)
                .authorizeHttpRequests(requests -> requests.anyRequest().access(onlyPlatformTokens()))
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiFilterChain(HttpSecurity http, ProblemErrorResponder problemErrorResponder,
            RateLimitProperties rateLimitProperties, ClientAddress clientAddress) throws Exception {
        return statelessResourceServer(http, problemErrorResponder)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .requestMatchers(HttpMethod.POST, PUBLIC_ROUTES.toArray(String[]::new)).permitAll()
                        .requestMatchers(HttpMethod.GET, DOCUMENTATION_ROUTES.toArray(String[]::new))
                        .permitAll()
                        .anyRequest().access(onlyOrganizationTokens()))
                .addFilterBefore(new RateLimitFilter(publicRouteMatcher(), rateLimitProperties,
                        clientAddress, problemErrorResponder), SecurityContextHolderFilter.class)
                .build();
    }

    private static HttpSecurity statelessResourceServer(HttpSecurity http,
            ProblemErrorResponder problemErrorResponder) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(problemErrorResponder)
                        .accessDeniedHandler(problemErrorResponder))
                .oauth2ResourceServer(resource -> resource
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(problemErrorResponder)
                        .accessDeniedHandler(problemErrorResponder))
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy.policyDirectives(CONTENT_SECURITY_POLICY))
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(HSTS_MAX_AGE_SECONDS))
                        .permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY)));
    }

    private static AuthorizationManager<RequestAuthorizationContext> onlyPlatformTokens() {
        return (authentication, context) -> new AuthorizationDecision(
                signedIn(authentication.get()) && fromThePlatform(authentication.get()));
    }

    private static AuthorizationManager<RequestAuthorizationContext> onlyOrganizationTokens() {
        return (authentication, context) -> new AuthorizationDecision(
                signedIn(authentication.get()) && !fromThePlatform(authentication.get()));
    }

    private static boolean signedIn(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && !TRUST.isAnonymous(authentication);
    }

    private static boolean fromThePlatform(Authentication authentication) {
        return authentication.getPrincipal() instanceof Jwt token
                && TokenClaims.PLATFORM_ACTOR.equals(token.getClaimAsString(TokenClaims.ACTOR_TYPE));
    }

    private static RequestMatcher publicRouteMatcher() {
        PathPatternRequestMatcher.Builder patterns = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(PUBLIC_ROUTES.stream()
                .map(route -> (RequestMatcher) patterns.matcher(HttpMethod.POST, route))
                .toList());
    }

    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(secretKey()));
    }

    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
    }

    private SecretKeySpec secretKey() {
        return new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthorityPrefix("SCOPE_");
        authorities.setAuthoritiesClaimName(TokenClaims.SCOPE);
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}

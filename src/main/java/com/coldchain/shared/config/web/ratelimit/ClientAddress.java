package com.coldchain.shared.config.web.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ClientAddress {

    private static final String FORWARDED_FOR = "X-Forwarded-For";

    private final Set<String> trustedProxies;

    public ClientAddress(RateLimitProperties properties) {
        this.trustedProxies = Set.copyOf(properties.trustedProxies());
    }

    public String of(HttpServletRequest request) {
        String socketAddress = request.getRemoteAddr();
        if (!trustedProxies.contains(socketAddress)) {
            return socketAddress;
        }
        String forwarded = request.getHeader(FORWARDED_FOR);
        if (forwarded == null || forwarded.isBlank()) {
            return socketAddress;
        }
        return forwarded.split(",")[0].trim();
    }
}

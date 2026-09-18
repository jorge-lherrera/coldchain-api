package com.coldchain.shared.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ProblemErrorResponder implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ProblemDetails problemDetails;

    private final ObjectMapper objectMapper;

    public ProblemErrorResponder(ProblemDetails problemDetails, ObjectMapper objectMapper) {
        this.problemDetails = problemDetails;
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        respond(request, response, CoreErrorCode.NOT_AUTHENTICATED, exception.getMessage());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        respond(request, response, CoreErrorCode.NOT_AUTHORIZED, exception.getMessage());
    }

    public void respond(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode,
            String detail) throws IOException {
        respond(request, response, errorCode, detail, Map.of());
    }

    public void respond(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode,
            String detail, Map<String, Object> extensions) throws IOException {
        ProblemDetail problem = problemDetails.describe(errorCode, detail, request.getRequestURI());
        extensions.forEach(problem::setProperty);
        write(response, problem);
    }

    private void write(HttpServletResponse response, ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(problem));
    }
}

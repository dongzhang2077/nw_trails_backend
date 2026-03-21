package ca.douglas.csis4280.nwtrails.config;

import ca.douglas.csis4280.nwtrails.common.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException {
        Integer errorStatus = resolveErrorStatus(request);
        if (errorStatus != null && errorStatus == HttpStatus.BAD_REQUEST.value()) {
            String originalPath = resolveOriginalRequestPath(request);
            writeErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                new ErrorResponse(
                    "VALIDATION_ERROR",
                    "Malformed JSON request body.",
                    Map.of("path", originalPath)
                )
            );
            return;
        }

        writeErrorResponse(
            response,
            HttpStatus.UNAUTHORIZED,
            new ErrorResponse(
                "UNAUTHORIZED",
                "Authentication is required to access this resource.",
                Map.of("path", request.getRequestURI())
            )
        );
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException exception
    ) throws IOException {
        writeErrorResponse(
            response,
            HttpStatus.FORBIDDEN,
            new ErrorResponse(
                "FORBIDDEN",
                "You do not have permission to access this resource.",
                Map.of("path", request.getRequestURI())
            )
        );
    }

    private void writeErrorResponse(
        HttpServletResponse response,
        HttpStatus status,
        ErrorResponse body
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private Integer resolveErrorStatus(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        if (statusCode instanceof Integer integerStatusCode) {
            return integerStatusCode;
        }
        return null;
    }

    private String resolveOriginalRequestPath(HttpServletRequest request) {
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        if (requestUri instanceof String uri && !uri.isBlank()) {
            return uri;
        }
        return request.getRequestURI();
    }
}

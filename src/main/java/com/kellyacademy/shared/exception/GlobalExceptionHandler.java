package com.kellyacademy.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // EXCEPCIONES DE RECURSOS

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {

        log.warn("Recurso no encontrado: {} - URI: {}", ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo("RESOURCE_NOT_FOUND")
                .mensaje(ex.getMessage())
                .estado(HttpStatus.NOT_FOUND.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {

        log.warn("Error de negocio [{}]: {} - URI: {}", ex.getCodigo(), ex.getMessage(), request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo(ex.getCodigo())
                .mensaje(ex.getMessage())
                .estado(HttpStatus.BAD_REQUEST.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // EXCEPCIONES DE VALIDACIÓN

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensaje = error.getDefaultMessage();
            errores.put(campo, mensaje);
        });

        log.warn("Error de validación en URI {}: {}", request.getRequestURI(), errores);

        ErrorResponse error = ErrorResponse.builder()
                .codigo("VALIDATION_ERROR")
                .mensaje("Error de validación en los datos enviados")
                .estado(HttpStatus.BAD_REQUEST.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .detalles(errores)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // EXCEPCIONES DE SEGURIDAD (lanzadas en filtros)

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request
    ) {

        log.warn("Credenciales inválidas en URI: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo("INVALID_CREDENTIALS")
                .mensaje("Correo o contraseña incorrectos")
                .estado(HttpStatus.UNAUTHORIZED.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabledException(
            DisabledException ex,
            HttpServletRequest request
    ) {

        log.warn("Usuario deshabilitado intentó acceder a URI: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo("USER_DISABLED")
                .mensaje("Tu cuenta está inactiva. Contacta al administrador.")
                .estado(HttpStatus.FORBIDDEN.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLockedException(
            LockedException ex,
            HttpServletRequest request
    ) {

        log.warn("Usuario bloqueado intentó acceder a URI: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo("USER_LOCKED")
                .mensaje("Tu cuenta está bloqueada. Contacta al administrador.")
                .estado(HttpStatus.FORBIDDEN.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {

        log.warn("Acceso denegado en URI: {}", request.getRequestURI());

        ErrorResponse error = ErrorResponse.builder()
                .codigo("ACCESS_DENIED")
                .mensaje("No tienes permisos para acceder a este recurso")
                .estado(HttpStatus.FORBIDDEN.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    // EXCEPCIÓN GENÉRICA (catch-all)

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Error inesperado en URI: {}", request.getRequestURI(), ex);

        ErrorResponse error = ErrorResponse.builder()
                .codigo("INTERNAL_ERROR")
                .mensaje("Ocurrió un error inesperado. Contacta al soporte.")
                .estado(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .ruta(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
package com.kellyacademy.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {

    private final String codigo;

    public BusinessException(String mensaje) {
        super(mensaje);
        this.codigo = "BUSINESS_ERROR";
    }

    public BusinessException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
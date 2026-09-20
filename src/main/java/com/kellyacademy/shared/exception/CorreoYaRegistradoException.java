package com.kellyacademy.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class CorreoYaRegistradoException extends RuntimeException {

    public CorreoYaRegistradoException(String correo) {
        super(String.format("Ya existe un usuario registrado con el correo: '%s'", correo));
    }
}
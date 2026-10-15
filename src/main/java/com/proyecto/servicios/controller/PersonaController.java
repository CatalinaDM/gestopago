package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @PostMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crearUser(
            @Valid @RequestBody GenericRequest<PersonasRequest> request) {
        return new ResponseEntity<>(personaService.creaPersona(request), HttpStatus.OK);
    }

    @PutMapping(value = "/personasActualiza", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(
            @Valid @RequestBody GenericRequest<PersonasRequest> request) {
        return new ResponseEntity<>(personaService.actualizaPersona(request), HttpStatus.OK);
    }

    @PutMapping(value = "/personasElimina", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> eliminaUser(
            @Valid @RequestBody GenericRequest<EliminaPersonaRequest> request) {
        return new ResponseEntity<>(personaService.eliminaPersona(request), HttpStatus.OK);
    }
}

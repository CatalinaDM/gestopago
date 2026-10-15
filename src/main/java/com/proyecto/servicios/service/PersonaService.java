package com.proyecto.servicios.service;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;

public interface PersonaService {
    PersonaResponse creaPersona(GenericRequest<PersonasRequest> request);
    GenericResponse eliminaPersona(GenericRequest<EliminaPersonaRequest> request);
    GenericResponse actualizaPersona(GenericRequest<PersonasRequest> request);
}

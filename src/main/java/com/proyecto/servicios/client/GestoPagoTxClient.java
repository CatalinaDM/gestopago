package com.proyecto.servicios.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "gestoPagoTx",
        url = "${gestopago.tx.url:${gestopago.product.url}}"
)
public interface GestoPagoTxClient {

    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    String sendTx(
            @RequestHeader("Authorization") String authorization,
            @RequestBody MultiValueMap<String, String> formParams
    );

    @PostMapping(
            value = "/sistema/service/sendTx.do",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    String sendTxConApiKey(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody MultiValueMap<String, String> formParams
    );
}




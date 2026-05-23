package com.contrabajo.comunicaciones_api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "ConTrabajo - API de Comunicaciones",
                version = "v1",
                description = "### Documentacion oficial de la API de comunicaciones.\n\n" +
                        "**Instrucciones de uso inicial:**\n" +
                        "1. Autenticate primero en usuarios-api para obtener un token JWT.\n" +
                        "2. Usa el boton **Authorize** de Swagger UI y pega el token con formato Bearer.\n" +
                        "3. Prueba los endpoints de chats, mensajes y reportes segun el rol requerido.\n\n" +
                        "---"
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "Introduzca el token JWT generado por usuarios-api prefijado con Bearer."
)
public class OpenApiConfig {
}

package pe.edu.upeu.cinerama.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Cinerama API")
                        .version("1.0.0")
                        .description("""
                                Sistema de gestion de cine - Documentacion de servicios REST.

                                Autenticacion: inicia sesion con POST /api/v1/auth/login y pega el token
                                en el boton Authorize (Bearer JWT).
                                Las operaciones de escritura (crear, actualizar, eliminar) y el panel de
                                administracion requieren rol ADMIN; un cliente solo puede comprar,
                                calificar y registrar incidencias propias.
                                El modulo de puerta requiere rol EMPLEADO.
                                """)
                        .contact(new Contact()
                                .name("Proyecto LP2 - Cinerama")
                                .email("@cinerama")))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token JWT devuelto por /api/v1/auth/login")));
    }
}
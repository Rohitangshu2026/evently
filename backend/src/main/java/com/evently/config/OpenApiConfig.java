package com.evently.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata. Declares the bearer-token scheme so Swagger UI
 * shows an Authorize button — paste an access token from {@code /auth/login}
 * and every try-it-out request carries it.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    /**
     * Builds the top-level OpenAPI description served at {@code /v3/api-docs}.
     *
     * @return the customized document skeleton
     */
    @Bean
    public OpenAPI eventlyOpenApi(){
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Access token from /api/v1/auth/login or /signup");

        return new OpenAPI()
                .info(new Info()
                        .title("Evently API")
                        .version("v1")
                        .description("Event ticketing REST API: organizer event management, "
                                + "public event discovery, oversell-safe ticket purchase, "
                                + "and staff entry validation."))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}

package com.isaac.approvalworkflowengine.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI approvalWorkflowOpenApi() {
        return new OpenAPI()
            .components(
                new Components().addSecuritySchemes(
                    "bearerAuth",
                    new SecurityScheme()
                        .name("bearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    OperationCustomizer apiVersionHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            boolean headerAlreadyPresent = operation.getParameters() != null
                && operation.getParameters().stream()
                .anyMatch(parameter ->
                    "header".equalsIgnoreCase(parameter.getIn())
                        && "API-Version".equalsIgnoreCase(parameter.getName())
                );

            if (!headerAlreadyPresent) {
                operation.addParametersItem(
                    new Parameter()
                        .name("API-Version")
                        .in("header")
                        .required(false)
                        .description("Semantic API version. Defaults to 1.0 when omitted.")
                        .schema(new StringSchema()._default("1.0"))
                );
            }

            return operation;
        };
    }
}

package com.isaac.approvalworkflowengine;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class OpenApiContractValidationTest {

    @Test
    void blueprintOpenApiContractParsesWithoutStructuralErrors() {
        Path contractPath = Path.of("docs", "blueprint", "openapi.yaml");
        assertThat(contractPath).exists();

        ParseOptions options = new ParseOptions();
        options.setResolve(false);

        SwaggerParseResult result = new OpenAPIParser().readLocation(
            contractPath.toUri().toString(),
            null,
            options
        );

        assertThat(result.getOpenAPI()).isNotNull();
        assertThat(result.getMessages()).isEmpty();
    }
}

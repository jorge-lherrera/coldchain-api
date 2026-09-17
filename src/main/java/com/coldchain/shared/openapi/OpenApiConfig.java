package com.coldchain.shared.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Map;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    static final String BEARER_SCHEME = "bearerAuth";

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final Map<String, String> SHARED_FAILURES = Map.of(
            "400", "The body could not be read or did not pass validation",
            "401", "The request carried no usable credentials",
            "403", "The credentials do not grant this operation",
            "404", "The resource does not exist or is not visible to the caller",
            "409", "The write collided with a uniqueness rule",
            "422", "A business rule rejected the request",
            "429", "The client exceeded the allowed number of attempts",
            "500", "The request could not be completed");

    @Bean
    OpenAPI coldChainOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("ColdChain API")
                        .version("v1")
                        .description("Cold-chain custody and compliance: organizations, shipments, "
                                + "temperature readings and the evidence a regulator asks for.")
                        .contact(new Contact().name("ColdChain")
                                .url("https://github.com/jorge-lherrera/coldchain-api")))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("The access token returned by POST /v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }

    @Bean
    OperationCustomizer everyOperationSharesTheProblemContract() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses() == null
                    ? new ApiResponses()
                    : operation.getResponses();
            SHARED_FAILURES.forEach((status, description) -> responses.addApiResponse(status,
                    new ApiResponse()
                            .description(description)
                            .content(new Content().addMediaType(PROBLEM_JSON, new MediaType()))));
            return operation.responses(responses);
        };
    }
}

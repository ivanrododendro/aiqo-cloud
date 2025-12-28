package org.iro.aiqo.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.iro.aiqo.model.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class SpringDataRestConfig implements RepositoryRestConfigurer {

    @Override
    public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.setBasePath("/api");
        cors.addMapping("/**")
                .allowedOrigins("http://localhost:4200") // Specific Angular dev server origin
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Location") // Important for REST responses
                .allowCredentials(true) // Changed to true if using cookies/auth
                .maxAge(3600);

        config.exposeIdsFor(
                Application.class,
                AIHint.class,
                AITask.class,
                Environment.class,
                LLM.class,
                LLMFamily.class,
                LogFile.class,
                Query.class,
                Run.class
        );
    }

    @Bean
    OpenAPI customOpenAPI(RepositoryRestConfiguration repositoryRestConfiguration) {
        OpenAPI openAPI = new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("AI Query Optimizer API")
                        .description("API for managing AI-powered query optimization tasks")
                        .version("1.0.0"));

        addApplicationSearchPath(openAPI, repositoryRestConfiguration);
        addEnvironmentSearchPath(openAPI, repositoryRestConfiguration);
        return openAPI;
    }

    private void addApplicationSearchPath(OpenAPI openAPI, RepositoryRestConfiguration repositoryRestConfiguration) {
        String path = buildPath(repositoryRestConfiguration, "/applications/search/byNameAndTenant");

        Operation operation = new Operation()
                .operationId("findApplicationByNameAndTenant")
                .summary("Search application by name and tenant id")
                .addTagsItem("application-entity-controller")
                .addParametersItem(new Parameter()
                        .name("name")
                        .in("query")
                        .required(true)
                        .schema(new StringSchema()))
                .addParametersItem(new Parameter()
                        .name("tenantId")
                        .in("query")
                        .required(true)
                        .schema(new IntegerSchema()))
                .responses(buildResponses(schemaRef("EntityModelApplication")));

        PathItem pathItem = openAPI.getPaths() != null && openAPI.getPaths().containsKey(path)
                ? openAPI.getPaths().get(path)
                : new PathItem();
        pathItem.setGet(operation);
        openAPI.path(path, pathItem);
    }

    private void addEnvironmentSearchPath(OpenAPI openAPI, RepositoryRestConfiguration repositoryRestConfiguration) {
        String path = buildPath(repositoryRestConfiguration, "/environments/search/byName");

        Operation operation = new Operation()
                .operationId("findEnvironmentByName")
                .summary("Search environments by name")
                .addTagsItem("environment-entity-controller")
                .addParametersItem(new Parameter()
                        .name("name")
                        .in("query")
                        .required(true)
                        .schema(new StringSchema()))
                .responses(buildResponses(schemaRef("EntityModelEnvironment")));

        PathItem pathItem = openAPI.getPaths() != null && openAPI.getPaths().containsKey(path)
                ? openAPI.getPaths().get(path)
                : new PathItem();
        pathItem.setGet(operation);
        openAPI.path(path, pathItem);
    }

    private ApiResponses buildResponses(Schema<?> schema) {
        ApiResponse success = new ApiResponse()
                .description("OK")
                .content(new Content()
                        .addMediaType("application/hal+json", new MediaType().schema(schema)));

        ApiResponse notFound = new ApiResponse().description("Not Found");

        return new ApiResponses()
                .addApiResponse("200", success)
                .addApiResponse("404", notFound);
    }

    private String buildPath(RepositoryRestConfiguration repositoryRestConfiguration, String suffix) {
        String basePath = repositoryRestConfiguration.getBasePath().toString();
        if (StringUtils.hasText(basePath) && !"/".equals(basePath)) {
            if (!basePath.startsWith("/")) {
                basePath = "/" + basePath;
            }
            if (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length() - 1);
            }
        } else {
            basePath = "";
        }
        return basePath + suffix;
    }

    private Schema<?> schemaRef(String schemaName) {
        return new Schema<>().$ref("#/components/schemas/" + schemaName);
    }
}

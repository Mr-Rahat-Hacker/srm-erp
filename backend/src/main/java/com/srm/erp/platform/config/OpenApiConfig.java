package com.srm.erp.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI srmOpenAPI() {
        return new OpenAPI()
                // Relative server URL keeps Swagger requests on the current origin.
                // This avoids localhost/http URL mismatches behind HTTPS Codespaces forwarding.
                .servers(List.of(new Server()
                        .url("/")
                        .description("Current SRM API server")))
                .info(new Info()
                        .title("SRM ERP API")
                        .description("""
                                REST API for the Supplier Relationship Management platform.
                                First-release scope includes supplier registration and supplier
                                collaboration workflows. SAP remains the system of record for
                                SAP Vendor Master and Material Master data.
                                """)
                        .version("v1")
                        .contact(new Contact()
                                .name("SRM Development Team"))
                        .license(new License()
                                .name("Internal project use")));
    }
}

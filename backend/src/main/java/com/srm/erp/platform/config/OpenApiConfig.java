package com.srm.erp.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI srmOpenAPI() {
        return new OpenAPI()
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

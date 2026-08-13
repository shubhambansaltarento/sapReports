package com.sapreport.dynpro.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dynproOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Dynpro Reporting API")
                .description("Metadata-driven report configuration and data APIs for TVS DMS "
                        + "(Dealer Ledger is the reference implementation; see docs/spec).")
                .version("0.1.0-draft"));
    }
}

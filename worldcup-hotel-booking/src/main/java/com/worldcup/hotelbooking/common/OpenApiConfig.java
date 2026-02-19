package com.worldcup.hotelbooking.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI employeeApi() {
        // غيروها الله يعينكم
        return new OpenAPI()
                .info(new Info()
                        .title("SWER313 Employee API")
                        .version("v1")
                        .description("Employee service API for pagination, sorting, and filtering lab."));
    }
}

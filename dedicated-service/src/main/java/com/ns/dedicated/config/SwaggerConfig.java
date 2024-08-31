package com.ns.dedicated.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("dedicated-proejct")
                .description("hello world")
                .version("1.0.0");

        return new OpenAPI()
                .info(info);

    }


}
package dev.ua.ikeepcalm.Geminify.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI applicationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Geminify API")
                        .description("AI-powered evaluation service for Minecraft server applications")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ikeepcalm")
                                .email("ikeepcalm@ukr.net")));
    }
}
package com.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI 知识工单平台 API 文档")
                        .version("1.0.0")
                        .description("智能客服与知识管理系统 API 接口文档")
                        .contact(new Contact()
                                .name("开发团队")
                                .email("support@example.com")));
    }
}
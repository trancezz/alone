package com.twotrance.alone.config;

import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

/**
 * WebMvcConfiguration
 *
 * @author trance
 * @description web mvc configuration
 * @date 2021/1/25
 */
@Configuration
public class WebMvcConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return (container -> container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"), new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500")));
    }
}

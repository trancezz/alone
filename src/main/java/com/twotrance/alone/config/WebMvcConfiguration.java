package com.twotrance.alone.config;

import cn.hutool.core.map.MapUtil;
import org.hibernate.Hibernate;
import org.hibernate.validator.HibernateValidatorConfiguration;
import org.springframework.boot.validation.MessageInterpolatorFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * WebMvcConfiguration
 *
 * @author trance
 * @description web mvc configuration
 * @date 2021/1/25
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableWebServerFactory> webServerFactoryCustomizer() {
        return (container -> container.addErrorPages(new ErrorPage(HttpStatus.NOT_FOUND, "/error/404"), new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/error/500")));
    }

    /**
     * 消息源配置
     *
     * @return ResourceBundleMessageSource
     */
    @Bean(name = "messageSource")
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setDefaultEncoding("utf-8");
        messageSource.setCacheMillis(-1);
        messageSource.setBasenames("i18n/ValidationMessages", "exceptions/exception");
        return messageSource;
    }

    /**
     * 配置参数验证配置文件i18n
     *
     * @return Validator 验证器
     */
    @Override
    public Validator getValidator() {
        LocalValidatorFactoryBean factoryBean = new LocalValidatorFactoryBean();
        MessageInterpolatorFactory interpolatorFactory = new MessageInterpolatorFactory();
        factoryBean.setMessageInterpolator(interpolatorFactory.getObject());
        factoryBean.setValidationMessageSource(messageSource());
        factoryBean.setValidationPropertyMap(MapUtil.of(HibernateValidatorConfiguration.FAIL_FAST, "true"));
        return factoryBean;
    }
}

package com.doctor.notify.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InternalAuthProperties.class)
public class WebFilterConfig {

    @Bean
    public FilterRegistrationBean<InternalTokenFilter> internalTokenFilter(InternalAuthProperties props) {
        FilterRegistrationBean<InternalTokenFilter> b = new FilterRegistrationBean<>();
        b.setFilter(new InternalTokenFilter(props));
        b.addUrlPatterns("/internal/*");
        b.setOrder(1);
        return b;
    }
}
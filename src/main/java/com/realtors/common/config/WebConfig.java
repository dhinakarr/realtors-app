package com.realtors.common.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.realtors.common.JwtFilter;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	
    private JwtFilter jwtFilter;

	public WebConfig(JwtFilter jwtFilte) {
		this.jwtFilter = jwtFilte;
	}
	
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
    	registry.addResourceHandler("/api/**").addResourceLocations("classpath:/none/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // All non-API paths go to index.html
        registry.addViewController("/{spring:[^.]+}")
                .setViewName("forward:/index.html");
        registry.addViewController("/**/{spring:[^.]+}")
                .setViewName("forward:/index.html");
    }
    
    @Bean
    public FilterRegistrationBean<JwtFilter> filterRegistrationBean() {
        FilterRegistrationBean<JwtFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(jwtFilter);
        registrationBean.addUrlPatterns("/api/*"); // filter all /api/* endpoints
        return registrationBean;
    }
}


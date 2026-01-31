package com.realtors.common.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final FileStorageProperties fileStorageProperties;
	private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

	public WebConfig(FileStorageProperties fileStorageProperties) {
		this.fileStorageProperties = fileStorageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		String uploadPath = fileStorageProperties.getUploadDir();
		String location = Paths.get(uploadPath).toAbsolutePath().toUri().toString();

		if (!location.endsWith("/")) {
	        location += "/";
	    }

		logger.info("@WebConfig.addResourceHandlers location: {}", location);
//		System.out.println("@WebConfig.addResourceHandlers FINAL UPLOAD PATH MAPPED: file:///" + uploadPath.replace("\\", "/"));
//		registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath);
//		registry.addResourceHandler("/files/**").addResourceLocations("file:///" + uploadPath + "/");

		registry.addResourceHandler("/files/**").addResourceLocations(location ) 
				.setCachePeriod(0).resourceChain(true);

		// Let Spring serve static files correctly
		registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
		registry.addResourceHandler("/.well-known/**").addResourceLocations("classpath:/static/.well-known/");
		//registry.setOrder(-1);
		// Let Spring map all other static files
		registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		// Root → index.html
		registry.addViewController("/").setViewName("forward:/index.html");

		// 3️⃣ SPA fallback — EXCLUDE uploads!
		registry.addViewController("/{path:^(?!uploads|files|assets|api|static|.*\\..*$).*$}")
				.setViewName("forward:/index.html");

		registry.addViewController("/{path:^(?!uploads|files|assets|api|static|.*\\..*$).*$}/**")
				.setViewName("forward:/index.html");

		/*
		 * registry.addViewController(
		 * "/{path:^(?!assets|api|static|uploads|.*\\..*$).*$}").setViewName(
		 * "forward:/index.html"); registry.addViewController(
		 * "/{path:^(?!assets|api|static|uploads|.*\\..*$).*$}/**").setViewName(
		 * "forward:/index.html");
		 */

		/*
		 * // SPA fallback for routes WITHOUT dots
		 * registry.addViewController("/{path:^(?!assets|api|static|.*\\..*$).*$}").
		 * setViewName("forward:/index.html");
		 * registry.addViewController("/{path:^(?!assets|api|static|.*\\..*$).*$}/**").
		 * setViewName("forward:/index.html");
		 */
		// Forward /error to index.html (avoid Whitelabel)
//        registry.addViewController("/error").setViewName("forward:/index.html");
	}
}

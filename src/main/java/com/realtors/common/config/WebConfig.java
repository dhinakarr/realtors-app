package com.realtors.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final FileStorageProperties fileStorageProperties;

	public WebConfig(FileStorageProperties fileStorageProperties) {
		this.fileStorageProperties = fileStorageProperties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {

		String uploadPath = fileStorageProperties.getUploadDir();	
		if (!uploadPath.endsWith("/")) {
			uploadPath = uploadPath + "/";
		}
		System.out.println("@WebConfig.addResourceHandlers FINAL UPLOAD PATH MAPPED: file:///" + uploadPath.replace("\\", "/"));
//		registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + uploadPath);
		registry.addResourceHandler("/files/**").addResourceLocations("file:///" + uploadPath + "/");

		// Let Spring serve static files correctly
		registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/static/assets/");
		registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/static/favicon.ico");
		registry.addResourceHandler("/.well-known/**").addResourceLocations("classpath:/static/.well-known/");

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

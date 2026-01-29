package com.realtors.alerts.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailTemplateService {

    private final TemplateEngine templateEngine;
    
    private static final Logger logger = LoggerFactory.getLogger(EmailTemplateService.class);

    public EmailTemplateService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildNotificationEmail(String title, String message, Map<String, Object> html) {
    	logger.info("@EmailTemplateService.buildNotificationEmail title: {}", title);
        Context context = new Context();
        context.setVariable("title", title);
        context.setVariable("message", message);
        context.setVariables(html);
        String template = (String) html.get("template");
        return templateEngine.process(template, context);
    }
}

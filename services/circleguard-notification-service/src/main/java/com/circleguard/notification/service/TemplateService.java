package com.circleguard.notification.service;

import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TemplateService {

    private final Configuration freemarkerConfig;

    @Value("${notification.url.testing:https://circleguard.example.com/testing}")
    private String testingUrl;

    @Value("${notification.url.isolation:https://circleguard.example.com/isolation}")
    private String isolationUrl;

    @Value("${notification.url.guidelines:circleguard://guidelines}")
    private String guidelinesDeepLink;

    public String generateEmailContent(String status, String userName) {
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("userName", userName != null ? userName : "User");
            model.put("status", status != null ? status : "UNKNOWN");
            model.put("testingUrl", testingUrl);
            model.put("isolationUrl", isolationUrl);
            
            Template t = freemarkerConfig.getTemplate("health_alert.ftl");
            return FreeMarkerTemplateUtils.processTemplateIntoString(t, model);
        } catch (Exception e) {
            return "CircleGuard Health Update: Your status is now " + status + ". Please check the app for instructions.";
        }
    }

    public String generatePushContent(String status) {
        if ("SUSPECT".equals(status)) {
            return "Health Alert: Your status is SUSPECT. Tap for isolation steps.";
        } else if ("PROBABLE".equals(status)) {
            return "Health Alert: You are now PROBABLE due to area exposure. Monitor symptoms and maintain distance.";
        }
        return "CircleGuard: Your health status has been updated to " + status;
    }

    public Map<String, String> generatePushMetadata(String status) {
        if ("SUSPECT".equals(status) || "PROBABLE".equals(status)) {
            return Map.of("url", guidelinesDeepLink);
        }
        return Map.of();
    }

    public String generateSmsContent(String status) {
        return "CircleGuard Alert: Your health status is now " + status + ". Please check your email for details and guidelines.";
    }
}

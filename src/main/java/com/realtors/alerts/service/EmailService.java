package com.realtors.alerts.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.realtors.alerts.exception.NotificationDeliveryException;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

	private final JavaMailSender mailSender;

	public EmailService(JavaMailSender mailSender) {
		this.mailSender = mailSender;
	}

	public void sendHtmlEmail(String to, String subject, String html) throws NotificationDeliveryException {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(html, true);
			helper.setFrom("apkdhina@gmail.com");
			helper.addInline("logo", new ClassPathResource("static/logo.png"));

			mailSender.send(message);

		} catch (Exception e) {
			throw new NotificationDeliveryException("EMAIL delivery failed", e);
		}
	}
}

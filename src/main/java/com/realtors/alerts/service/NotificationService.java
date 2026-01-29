package com.realtors.alerts.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.dto.RecipientDetail;
import com.realtors.alerts.messages.NotificationMessage;
import com.realtors.alerts.sender.NotificationSender;


@Service
public class NotificationService {

	private final Map<NotificationChannel, NotificationSender> senders;
	private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
	
	public NotificationService(List<NotificationSender> senderList) {

        this.senders = senderList.stream()
            .collect(Collectors.toMap(
                NotificationSender::channel,
                Function.identity()
            ));
    }

	public void send(List<NotificationInstruction> instructions) {
		for (NotificationInstruction instruction : instructions) {
			for (NotificationMessage msg : instruction.messages()) {
				RecipientDetail recipient = instruction.recipient();
				
				NotificationChannel channel = msg.getChannel();
	            NotificationSender sender = senders.get(channel);
	            String recipientStr = getRecipient(channel, recipient);

	            if (recipient.email() != null)
					sender.send(instruction, recipientStr);
			}
		}
		logger.info("@NotificationService.send messages Sent");
	}
	private String getRecipient(NotificationChannel channel, RecipientDetail recipt) {
		switch (channel) {
			case NotificationChannel.PUSH:
				return recipt.userId().toString();
			case NotificationChannel.EMAIL:
				return recipt.email();
			case NotificationChannel.WHATSAPP:
				return recipt.mobile();
			case NotificationChannel.SMS:
				return recipt.mobile();	
			default:
				return recipt.userId().toString();
			}
	}
}

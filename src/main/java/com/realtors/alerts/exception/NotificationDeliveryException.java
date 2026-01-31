package com.realtors.alerts.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NotificationDeliveryException extends Exception {
	private static final long serialVersionUID = -6493397687441986064L;
	private static final Logger logger = LoggerFactory.getLogger(NotificationDeliveryException.class);
	
	public NotificationDeliveryException(String message, Exception ex) {
		logger.error("NotificationDeliveryException error in method {}",
                   ex.getMessage());
	}
}

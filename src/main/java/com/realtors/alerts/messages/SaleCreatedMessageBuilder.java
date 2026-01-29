package com.realtors.alerts.messages;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.realtors.admin.dto.RoleType;
import com.realtors.admin.service.UserAuthService;
import com.realtors.admin.service.UserService;
import com.realtors.alerts.domain.event.EventType;
import com.realtors.alerts.domain.event.SaleCreatedEvent;
import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.dto.RecipientDetail;
import com.realtors.common.util.AppUtil;
import com.realtors.dashboard.dto.SaleDetailDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SaleCreatedMessageBuilder {

	private final UserService userService;
	private final UserAuthService authService;
	private static final Logger logger = LoggerFactory.getLogger(SaleCreatedMessageBuilder.class);

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	public List<NotificationInstruction> build(SaleCreatedEvent event) {
		logger.info("@SaleCreatedMessageBuilder.build frontendBaseUrl: {}", frontendBaseUrl);
		return List.of(customerMessage(event), agentMessage(event), financeMessage(event), mdMessage(event));
	}

	private NotificationInstruction customerMessage(SaleCreatedEvent event) {
		RecipientDetail recipient = authService.getRecipientDetail(event.getSaleDetails().getCustomerId());
		return new NotificationInstruction(RoleType.CUSTOMER, recipient, event.getEventId(), event.getEventtype(),
				List.of(
						new NotificationMessage(NotificationChannel.PUSH, "Plot booked 🎉",
								createCustomerMessage(event.getSaleDetails()), null),
						new NotificationMessage(NotificationChannel.EMAIL, "Plot Booking Confirmation", null,
								buildEmailContext(event.getSaleDetails()))
						)
				);
	}

	private NotificationInstruction agentMessage(SaleCreatedEvent event) {
		RecipientDetail recipient = authService.getRecipientDetail(event.getSaleDetails().getAgentId());

		return new NotificationInstruction(RoleType.PA, recipient, event.getEventId(), event.getEventtype(),
				List.of(
						new NotificationMessage(NotificationChannel.PUSH, "Plot booked 🎉",
								createCustomerMessage(event.getSaleDetails()), null),
						new NotificationMessage(NotificationChannel.EMAIL, "Plot Booking Confirmation", null,
								buildEmailContext(event.getSaleDetails()))
						)
				);
	}

	private NotificationInstruction financeMessage(SaleCreatedEvent event) {
		UUID financeUser = userService.getUsersByRoles(Set.of(RoleType.FINANCE.name())).getFirst().getUserId();
		RecipientDetail recipient = authService.getRecipientDetail(financeUser);

		return new NotificationInstruction(RoleType.PA, recipient, event.getEventId(), event.getEventtype(),
				List.of(
						new NotificationMessage(NotificationChannel.PUSH, "Plot booked 🎉",
								createCustomerMessage(event.getSaleDetails()), null),
						new NotificationMessage(NotificationChannel.EMAIL, "Plot Booking Confirmation", null,
								buildEmailContext(event.getSaleDetails()))
						)
				);
	}

	private NotificationInstruction mdMessage(SaleCreatedEvent event) {
		UUID financeUser = userService.getUsersByRoles(Set.of(RoleType.MD.name())).getFirst().getUserId();
		RecipientDetail recipient = authService.getRecipientDetail(financeUser);
		
		return new NotificationInstruction(RoleType.PA, recipient, event.getEventId(), event.getEventtype(),
				List.of(
						new NotificationMessage(NotificationChannel.PUSH, "Plot booked 🎉",
								createCustomerMessage(event.getSaleDetails()), null),
						new NotificationMessage(NotificationChannel.EMAIL, "Plot Booking Confirmation", null,
								buildEmailContext(event.getSaleDetails()))
						)
				);
	}

	private String createCustomerMessage(SaleDetailDTO data) {
		return String.format(
				"Dear %s, Your Registration for Plot No.%s in the %s Project at Village is fixed on %s by %s Associate."
				+ " Kindly Follow up with our Associate for further details. Regards: Diamond Realty Team.",
				data.getCustomerName(), data.getPlotNumber(), data.getProjectName(), AppUtil.getFormattedDate(LocalDate.now()),
				data.getAgentName());
	}

	public Map<String, Object> buildEmailContext(SaleDetailDTO data) {
		Map<String, Object> ctx = new HashMap<>();
		ctx.put("eventType", EventType.SALE_CREATED.name());
		ctx.put("template", "email/Sale_Created");
		ctx.put("customerName", data.getCustomerName());
		ctx.put("plotNumber", data.getPlotNumber());
		ctx.put("projectName", data.getProjectName());
		ctx.put("validTill", LocalDate.now().plusDays(10).format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
		ctx.put("saleId", data.getSaleId());
		ctx.put("logoUrl", frontendBaseUrl + "/assets/logo.png");
		return ctx;
	}
}

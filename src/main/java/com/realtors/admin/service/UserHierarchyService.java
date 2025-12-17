package com.realtors.admin.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.AppUserDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserHierarchyService {

	private final UserService userService;
	private static final Logger logger = LoggerFactory.getLogger(UserHierarchyService.class);

	public List<AppUserDto> getHierarchyUpwards(UUID userId) {
		List<AppUserDto> chain = new ArrayList<>();

		AppUserDto current = userService.findById(userId).orElse(null);
		if (current == null) {
			logger.error("User not found for ID: " + userId);
			return chain;
		}

		while (current.getManagerId() != null) {
			UUID managerId = current.getManagerId();
			AppUserDto manager = userService.findById(managerId).orElse(null);
			if (manager == null) {
				logger.error("Manager not found for ID: " + managerId + ", breaking chain.");
				break; // prevent NullPointerException
			}
			chain.add(current);
			current = manager;
		}
		return chain;
	}

	public List<UUID> getAllSubordinates(UUID userId) {
		List<UUID> list = new ArrayList<>();
		userService.findSubordinatesRecursive(userId, list);
		return list;
	}
	
	  public List<UUID> getUpwardsHierarchy(UUID userId) { 
		  return userService.getHierarchyUpwards(userId); 
	 }
}

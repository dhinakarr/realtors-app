package com.realtors.admin.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class UserTreeDto {
	private UUID userId;
    private String userName;

    private List<UserTreeDto> children = new ArrayList<>();
    
    public UserTreeDto(UUID userId, String userName) {
    	this.userId = userId;
    	this.userName = userName;
    }
}

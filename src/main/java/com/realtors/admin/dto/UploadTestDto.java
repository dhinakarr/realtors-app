package com.realtors.admin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor
@NoArgsConstructor
public class UploadTestDto {
	
	private UUID userId;
    private String email;
    private String mobile;
    private String fullName;
    private String address;
    private byte[] profileImage;        // BYTEA → byte[]
    private String metaJson;

}

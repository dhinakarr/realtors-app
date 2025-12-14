package com.realtors.customers.dto;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CustomerDto {
    private UUID customerId;
    private String customerName;
    private String email;
    private Long mobile;
    private UUID roleId;
    private Date dateOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Long altMobile;
    private String occupation;
    private String profileImagePath;
    private String notes;
    private String status;
    private UUID createdBy;
    private UUID updatedBy;
    private List<CustomerDocumentDto> documents = new ArrayList<>();
    
    public CustomerDto(UUID customerId, String customerName, Long mobile, UUID createdBy) {
    	this.customerId = customerId;
    	this.customerName = customerName;
    	this.mobile = mobile;
    	this.createdBy = createdBy;
    }
}

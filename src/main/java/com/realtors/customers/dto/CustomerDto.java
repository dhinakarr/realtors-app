package com.realtors.customers.dto;

import java.sql.Date;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {
    private UUID customerId;
    private String customerName;
    private String email;
    private long mobile;
    private Date dataOfBirth;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private long altMobile;
    private String occupation;
    private String profileImagePath;
    private String notes;
    private String status;
    private UUID createdBy;
    private UUID updatedBy;
    private List<CustomerDocumentDto> documents;
}

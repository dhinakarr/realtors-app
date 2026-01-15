package com.realtors.projects.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PlotUnitStatus {
	private UUID projectId;
	private int total;
	private int available;
	private int booked;
	private int sold;
	private int cancelled;
}

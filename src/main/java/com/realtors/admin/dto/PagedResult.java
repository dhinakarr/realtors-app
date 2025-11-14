package com.realtors.admin.dto;

import java.util.List;

public record PagedResult<T>(
	    List<T> data,
	    int page,
	    int size,
	    int total,
	    int totalPages
	) {}

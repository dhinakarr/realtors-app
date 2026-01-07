package com.realtors.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PagedResponse<T> {
    private List<T> data;
    private long total;
    private int page;
    private int size;
    private int totalPages;
}

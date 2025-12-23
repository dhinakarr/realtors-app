package com.realtors.sitevisit.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PageResponse<T> {

    private List<T> content;
    private long totalElements;
    private int page;
    private int size;
    private int totalPages;

    public PageResponse(List<T> content, long totalElements, int page, int size) {
        this.content = content;
        this.totalElements = totalElements;
        this.page = page;
        this.size = size;
        this.totalPages = (int) Math.ceil((double) totalElements / size);
    }
}

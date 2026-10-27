package com.example.banking.api;

import java.util.List;

public record PaginationResponse<T>(
        int currentPage, int pageSize, long totalRecords, int totalPages, List<T> data) {
}

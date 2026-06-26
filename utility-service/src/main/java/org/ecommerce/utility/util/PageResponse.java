package org.ecommerce.utility.util;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PageResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private LocalDateTime timestamp;


    public static <T> PageResponse<T> success(List<T> content, int page, int size, long totalElements, int totalPages) {
        return new  PageResponse<T>(content, page, size, totalElements, totalPages, LocalDateTime.now());
    }


}
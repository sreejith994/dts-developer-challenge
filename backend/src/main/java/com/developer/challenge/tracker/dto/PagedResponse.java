package com.developer.challenge.tracker.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "A page of results")
public record PagedResponse<T>(

		@Schema(description = "Items on this page")
		List<T> content,

		@Schema(description = "Zero-based page index", example = "0")
		int page,

		@Schema(description = "Requested page size", example = "20")
		int size,

		@Schema(description = "Total number of items across all pages", example = "42")
		long totalElements,

		@Schema(description = "Total number of pages", example = "3")
		int totalPages
) {

	public static <T> PagedResponse<T> from(Page<T> page) {
		return new PagedResponse<>(
				page.getContent(),
				page.getNumber(),
				page.getSize(),
				page.getTotalElements(),
				page.getTotalPages()
		);
	}

}

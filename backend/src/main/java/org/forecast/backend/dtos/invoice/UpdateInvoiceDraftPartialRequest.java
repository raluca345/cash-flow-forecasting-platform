package org.forecast.backend.dtos.invoice;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceDraftPartialRequest {

    @Valid
    private List<CreateInvoiceItemRequest> items;

    @PastOrPresent(message = "Issue date cannot be in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @FutureOrPresent(message = "Due date must be today or in the future")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;
}

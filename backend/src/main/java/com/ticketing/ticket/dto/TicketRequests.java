package com.ticketing.ticket.dto;

import com.ticketing.ticket.domain.TicketCategory;
import jakarta.validation.constraints.*;
import java.util.List;

public final class TicketRequests {
    private TicketRequests() { }
    public record Create(@NotNull @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String requestId,
                         @NotNull String originalText, @NotNull TicketCategory category,
                         @Positive Long relatedTicketId, @Size(max = 5) List<@NotNull String> attachmentTokens,
                         @Size(max = 120) String permissionName) {
        public Create(String requestId, String originalText, TicketCategory category, Long relatedTicketId, List<String> attachmentTokens) {
            this(requestId, originalText, category, relatedTicketId, attachmentTokens, null);
        }
    }
    public record Version(@NotNull @PositiveOrZero Long version) { }
    public record Assign(@NotNull @PositiveOrZero Long version, @NotNull @Positive Long assigneeId, String reason) { }
    public record Category(@NotNull @PositiveOrZero Long version, @NotNull TicketCategory category, @NotNull String reason) { }
    public record Comment(@NotNull @PositiveOrZero Long version, String content,
                          @Size(max = 5) List<@NotNull String> attachmentTokens) { }
    public record Result(@NotNull @PositiveOrZero Long version, @NotNull String resolutionCode, @NotNull String content,
                         @Size(max = 64) String permissionCode) {
        public Result(Long version, String resolutionCode, String content) { this(version,resolutionCode,content,null); }
    }
    public record Rework(@NotNull @PositiveOrZero Long version, @NotNull String reason) { }
}

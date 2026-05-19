package com.passfail.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatSessionItemResponse {
    private Long sessionId;
    private String title;
    private String preview;
    private String updatedAt;
    private String latestRole;
}

package com.passfail.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatSessionDetailResponse {
    private boolean success;
    private Long sessionId;
    private String title;
    private String handoffStatus;
    private List<AiChatMessageItemResponse> messages;
}

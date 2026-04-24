package com.passfail.post.dto;

import java.time.LocalDateTime;

import com.passfail.entity.CommentEntity;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommentResponseDTO {
	
	private Long commentId;
    private Long memberId;
    private String authorNickname;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    public static CommentResponseDTO from(CommentEntity comment) {
    	
    	return CommentResponseDTO.builder()
    			.commentId(comment.getCommentId())
    			.memberId(comment.getMemberId())
    			.authorNickname(comment.getMember() != null ? comment.getMember().getUsername() : "알 수 없음")
    			.content(comment.getContent())
    			.createdAt(comment.getCreatedAt())
    			.updatedAt(comment.getUpdatedAt())
    			.build();
    	
    }
	
}

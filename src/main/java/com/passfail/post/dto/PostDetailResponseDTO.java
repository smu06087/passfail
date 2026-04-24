package com.passfail.post.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.passfail.entity.PostEntity;
import com.passfail.enums.PostCategory;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostDetailResponseDTO {
	
	private Long postId;
    private Long memberId;
    private String authorNickname;  
    private PostCategory category;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private Boolean isLikedByCurrentUser;   // 현재 로그인 유저의 좋아요 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDTO> comments;
    
    public static PostDetailResponseDTO from(PostEntity post,
            								 boolean isLikedByCurrentUser,
            								 List<CommentResponseDTO> comments) {
    	
    	return PostDetailResponseDTO.builder()
    			.postId(post.getPostId())
    			.memberId(post.getMemberId())
    			.authorNickname(post.getMember() != null ? post.getMember().getUsername() : "알 수 없음")
    			.category(post.getCategory())
    			.title(post.getTitle())
    			.content(post.getContent())
    			.viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .isLikedByCurrentUser(isLikedByCurrentUser)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .build();
    	
    }
	
}

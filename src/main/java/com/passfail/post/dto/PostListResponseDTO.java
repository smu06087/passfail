package com.passfail.post.dto;

import java.time.LocalDateTime;

import com.passfail.entity.PostEntity;
import com.passfail.enums.PostCategory;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class PostListResponseDTO {
	
	private Long postId;
    private String authorNickname;   // MemberEntity에서 가져올 닉네임
    private PostCategory category;
    private String title;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private LocalDateTime createdAt;
    
    public static PostListResponseDTO from(PostEntity post) {
    	
    	return PostListResponseDTO.builder()
    			.postId(post.getPostId())
                .authorNickname(post.getMember() != null ? post.getMember().getUsername() : "알 수 없음")
                .category(post.getCategory())
                .title(post.getTitle())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .isPinned(post.getIsPinned())
                .createdAt(post.getCreatedAt())
                .build();
    }
	
}

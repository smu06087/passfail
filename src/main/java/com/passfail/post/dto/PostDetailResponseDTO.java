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
    private String authorName;
    private String authorNickname;  
    private PostCategory category;
    private String title;
    private String content;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean isPinned;
    private Boolean isLikedByCurrentUser;
    private Boolean isAuthor;  // ✅ 필드 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CommentResponseDTO> comments;
    
    // ✅ 기존 메서드 (오버로드 1) - currentMemberId 없이
    public static PostDetailResponseDTO from(PostEntity post,
            String currentUsername, // Long 대신 String
            boolean isLikedByCurrentUser,
            List<CommentResponseDTO> comments) {

	// 글쓴이의 username 가져오기
	String authorUsername = post.getMember() != null ? post.getMember().getUsername() : null;
	
	return PostDetailResponseDTO.builder()
	.postId(post.getPostId())
	.memberId(post.getMemberId())
	.authorName(authorUsername != null ? authorUsername : "알 수 없음")
	.authorNickname(post.getMember() != null ? post.getMember().getUsername() : "알 수 없음")
	.category(post.getCategory())
	.title(post.getTitle())
	.content(post.getContent())
	.viewCount(post.getViewCount())
	.likeCount(post.getLikeCount())
	.commentCount(post.getCommentCount())
	.isPinned(post.getIsPinned())
	.isLikedByCurrentUser(isLikedByCurrentUser)
	// ✅ 여기서 String(username)끼리 비교!!
	.isAuthor(currentUsername != null && currentUsername.equals(authorUsername))
	.createdAt(post.getCreatedAt())
	.updatedAt(post.getUpdatedAt())
	.comments(comments)
	.build();
	}
}
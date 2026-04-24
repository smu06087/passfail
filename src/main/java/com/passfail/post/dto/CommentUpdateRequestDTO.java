package com.passfail.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ✅ CommentCreateRequestDTO와 분리
// - Create: 향후 parentCommentId(대댓글) 등 필드가 추가될 수 있음
// - Update: 오직 content만 수정 가능 → 책임 명확히 구분
@Getter
@NoArgsConstructor
public class CommentUpdateRequestDTO {

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 1000, message = "댓글은 1000자 이하로 입력해주세요.")
    private String content;
}
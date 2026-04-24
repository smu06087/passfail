package com.passfail.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateRequestDTO {
	
	@NotBlank(message = "내용은 필수입니다.")
	@Size(max = 500, message = "내용은 500자 이하여야 합니다.")
	private String content;
	
}

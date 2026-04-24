package com.passfail.post.controller;

import com.passfail.post.dto.CommentCreateRequestDTO;
import com.passfail.post.dto.CommentUpdateRequestDTO; // ✅ 신규 DTO 사용
import com.passfail.post.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // POST /posts/{postId}/comments
    @PostMapping("/{postId}/comments")
    public ResponseEntity<Void> createComment(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody CommentCreateRequestDTO dto,
            @AuthenticationPrincipal Long memberId) {

        Long commentId = commentService.createComment(postId, dto, memberId);
        return ResponseEntity.created(
                URI.create("/posts/" + postId + "/comments/" + commentId)).build();
    }

    // PUT /posts/{postId}/comments/{commentId}
    // ✅ 수정: CommentCreateRequestDTO → CommentUpdateRequestDTO 분리
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable(name = "postId")    Long postId,
            @PathVariable(name = "commentId") Long commentId,
            @Valid @RequestBody CommentUpdateRequestDTO dto, // ✅ 변경
            @AuthenticationPrincipal Long memberId) {

        commentService.updateComment(commentId, dto, memberId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /posts/{postId}/comments/{commentId}
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable(name = "postId")    Long postId,
            @PathVariable(name = "commentId") Long commentId,
            @AuthenticationPrincipal Long memberId) {

        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.noContent().build();
    }
}
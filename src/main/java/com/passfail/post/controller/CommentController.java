package com.passfail.post.controller;

import com.passfail.entity.MemberEntity;
import com.passfail.member.repository.MemberRepository;
import com.passfail.post.dto.CommentCreateRequestDTO;
import com.passfail.post.dto.CommentUpdateRequestDTO;
import com.passfail.post.exception.UnauthorizedPostAccessException;
import com.passfail.post.service.CommentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final MemberRepository memberRepository; // ✅ 추가

    @PostMapping("/{postId}/comments")
    public ResponseEntity<Void> createComment(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody CommentCreateRequestDTO dto,
            Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        Long commentId = commentService.createComment(postId, dto, memberId);
        return ResponseEntity.created(
                URI.create("/posts/" + postId + "/comments/" + commentId)).build();
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable(name = "postId")    Long postId,
            @PathVariable(name = "commentId") Long commentId,
            @Valid @RequestBody CommentUpdateRequestDTO dto,
            Authentication authentication) {
        commentService.updateComment(commentId, dto, extractMemberId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable(name = "postId")    Long postId,
            @PathVariable(name = "commentId") Long commentId,
            Authentication authentication) {
        commentService.deleteComment(commentId, extractMemberId(authentication));
        return ResponseEntity.noContent().build();
    }

    // ✅ 3-case 처리: PostController와 동일
    private Long extractMemberId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("CommentController extractMemberId: not authenticated");
            throw new UnauthorizedPostAccessException();
        }

        Object principal = authentication.getPrincipal();
        log.info("CommentController extractMemberId: principal type = {}", 
                principal.getClass().getSimpleName());

        // Case 1: MemberEntity
        if (principal instanceof MemberEntity) {
            MemberEntity member = (MemberEntity) principal;
            log.info("Case 1: MemberEntity → memberId = {}", member.getMemberId());
            return member.getMemberId();
        }

        // Case 2: UserDetails
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            log.info("Case 2: UserDetails username = {}", username);
            return memberRepository.findByUsername(username)
                    .map(MemberEntity::getMemberId)
                    .orElseThrow(UnauthorizedPostAccessException::new);
        }

        // Case 3: String
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
            String username = (String) principal;
            log.info("Case 3: String username = {}", username);
            return memberRepository.findByUsername(username)
                    .map(MemberEntity::getMemberId)
                    .orElseThrow(UnauthorizedPostAccessException::new);
        }

        log.warn("CommentController extractMemberId: unknown principal type");
        throw new UnauthorizedPostAccessException();
    }
}
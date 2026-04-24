package com.passfail.post.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.passfail.entity.CommentEntity;
import com.passfail.entity.PostEntity;
import com.passfail.post.dto.CommentCreateRequestDTO;
import com.passfail.post.dto.CommentUpdateRequestDTO; // ✅ 신규
import com.passfail.post.exception.CommentNotFoundException; // ✅ 신규
import com.passfail.post.exception.PostNotFoundException;
import com.passfail.post.exception.UnauthorizedPostAccessException;
import com.passfail.post.repository.CommentRepository;
import com.passfail.post.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository    postRepository;

    // ── 댓글 작성 ──────────────────────────────────────────────────
    @Transactional
    public Long createComment(Long postId, CommentCreateRequestDTO dto, Long memberId) {
        PostEntity post = getActivePost(postId);

        CommentEntity comment = CommentEntity.builder()
                .postId(post.getPostId())
                .memberId(memberId)
                .content(dto.getContent())
                .build();

        commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);

        return comment.getCommentId();
    }

    // ── 댓글 수정 ──────────────────────────────────────────────────
    // ✅ 수정: CommentCreateRequestDTO → CommentUpdateRequestDTO 로 파라미터 변경
    @Transactional
    public void updateComment(Long commentId, CommentUpdateRequestDTO dto, Long currentMemberId) {
        CommentEntity comment = getActiveComment(commentId);
        validateCommentAuthor(comment, currentMemberId);

        comment.setContent(dto.getContent());
    }

    // ── 댓글 삭제 (소프트 삭제) ────────────────────────────────────
    @Transactional
    public void deleteComment(Long commentId, Long currentMemberId) {
        CommentEntity comment = getActiveComment(commentId);
        validateCommentAuthor(comment, currentMemberId);

        comment.setIsDeleted(true);
        postRepository.decrementCommentCount(comment.getPostId());
    }

    // ── 내부 헬퍼 ──────────────────────────────────────────────────
    private PostEntity getActivePost(Long postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    // ✅ 수정: RuntimeException → CommentNotFoundException (커스텀 예외)
    private CommentEntity getActiveComment(Long commentId) {
        return commentRepository.findById(commentId)
                .filter(c -> !c.getIsDeleted())
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void validateCommentAuthor(CommentEntity comment, Long currentMemberId) {
        if (!comment.getMemberId().equals(currentMemberId)) {
            throw new UnauthorizedPostAccessException();
        }
    }
}
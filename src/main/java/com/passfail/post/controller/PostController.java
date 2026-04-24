package com.passfail.post.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.passfail.enums.PostCategory;
import com.passfail.post.dto.PostCreateRequestDTO;
import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.dto.PostListResponseDTO;
import com.passfail.post.dto.PostUpdateRequestDTO;
import com.passfail.post.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // GET /posts?category=FREE&keyword=스프링&page=0&size=10
    @GetMapping
    public ResponseEntity<Page<PostListResponseDTO>> getPostList(
            @RequestParam(name = "category", required = false) PostCategory category,
            @RequestParam(name = "keyword",  required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostListResponseDTO> result = (keyword != null && !keyword.isBlank())
                ? postService.searchPosts(category, keyword, pageable)
                : postService.getPostList(category, pageable);

        return ResponseEntity.ok(result);
    }

    // GET /posts/{postId}
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDTO> getPostDetail(
            @PathVariable(name = "postId") Long postId,
            @AuthenticationPrincipal Long currentMemberId) {

        return ResponseEntity.ok(postService.getPostDetail(postId, currentMemberId));
    }

    // POST /posts
    @PostMapping
    public ResponseEntity<Void> createPost(
            @Valid @RequestBody PostCreateRequestDTO dto,
            @AuthenticationPrincipal Long memberId) {

        Long postId = postService.createPost(dto, memberId);
        // ✅ 수정: /api/posts → /posts 로 통일 (CommentController와 동일한 prefix 기준)
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    // PUT /posts/{postId}
    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody PostUpdateRequestDTO dto,
            @AuthenticationPrincipal Long memberId) {

        postService.updatePost(postId, dto, memberId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /posts/{postId}
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable(name = "postId") Long postId,
            @AuthenticationPrincipal Long memberId) {

        postService.deletePost(postId, memberId);
        return ResponseEntity.noContent().build();
    }

    // POST /posts/{postId}/like
    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable(name = "postId") Long postId,
            @AuthenticationPrincipal Long memberId) {

        boolean liked = postService.toggleLike(postId, memberId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    // PATCH /posts/{postId}/pin  ← 관리자 전용 (추후 @PreAuthorize 추가 예정)
    @PatchMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(
            @PathVariable(name = "postId") Long postId) {
        postService.togglePin(postId);
        return ResponseEntity.noContent().build();
    }
}
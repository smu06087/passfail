package com.passfail.post.controller;

import java.net.URI;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.passfail.entity.MemberEntity;
import com.passfail.enums.PostCategory;
import com.passfail.member.repository.MemberRepository;
import com.passfail.post.dto.PostCreateRequestDTO;
import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.dto.PostListResponseDTO;
import com.passfail.post.dto.PostUpdateRequestDTO;
import com.passfail.post.service.PostService;

import org.springframework.security.core.GrantedAuthority;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final MemberRepository memberRepository; // ✅ 추가: username으로 memberId 조회

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

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponseDTO> getPostDetail(
            @PathVariable(name = "postId") Long postId,
            Authentication authentication) {
        
        // ✅ 수정: extractMemberId(Long) 대신 username(String)을 추출해서 전달
        String currentUsername = (authentication != null && authentication.isAuthenticated()) 
                                 ? authentication.getName() 
                                 : null;

        return ResponseEntity.ok(postService.getPostDetail(postId, currentUsername));
    }

    @PostMapping
    public ResponseEntity<Void> createPost(
            @Valid @RequestBody PostCreateRequestDTO dto,
            Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        // ✅ NOTICE 카테고리는 ROLE_ADMIN 권한만 허용
        if (dto.getCategory() == PostCategory.NOTICE) {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch(auth -> auth.equals("ROLE_ADMIN"));

            if (!isAdmin) {
                return ResponseEntity.status(403).build();
            }
        }

        Long postId = postService.createPost(dto, memberId);
        return ResponseEntity.created(URI.create("/posts/" + postId)).build();
    }

    @PutMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @PathVariable(name = "postId") Long postId,
            @Valid @RequestBody PostUpdateRequestDTO dto,
            Authentication authentication) {
        postService.updatePost(postId, dto, extractMemberId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable(name = "postId") Long postId,
            Authentication authentication) {
        postService.deletePost(postId, extractMemberId(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Map<String, Boolean>> toggleLike(
            @PathVariable(name = "postId") Long postId,
            Authentication authentication) {
        Long memberId = extractMemberId(authentication);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }
        boolean liked = postService.toggleLike(postId, memberId);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @PatchMapping("/{postId}/pin")
    public ResponseEntity<Void> togglePin(
            @PathVariable(name = "postId") Long postId) {
        postService.togglePin(postId);
        return ResponseEntity.noContent().build();
    }

    // ✅ 3-case 처리: MemberEntity → UserDetails → String
    private Long extractMemberId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("extractMemberId: not authenticated");
            return null;
        }

        Object principal = authentication.getPrincipal();
        log.info("extractMemberId: principal type = {}", principal.getClass().getSimpleName());

        // Case 1: principal이 MemberEntity (로컬 로그인)
        if (principal instanceof MemberEntity) {
            MemberEntity member = (MemberEntity) principal;
            log.info("Case 1: MemberEntity → memberId = {}", member.getMemberId());
            return member.getMemberId();
        }

        // Case 4: principal이 OAuth2Member (소셜 로그인)
        if (principal instanceof com.passfail.member.dto.OAuth2Member) {
            String username = ((com.passfail.member.dto.OAuth2Member) principal).getName();
            log.info("Case 4: OAuth2Member name = {}", username);
            return memberRepository.findByUsername(username)
                    .map(MemberEntity::getMemberId)
                    .orElse(null);
        }

        // Case 2: principal이 UserDetails (username으로 DB 조회)
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            log.info("Case 2: UserDetails username = {}", username);
            return memberRepository.findByUsername(username)
                    .map(MemberEntity::getMemberId)
                    .orElse(null);
        }

        // Case 3: principal이 String (OAuth2 등)
        if (principal instanceof String && !"anonymousUser".equals(principal)) {
            String username = (String) principal;
            log.info("Case 3: String username = {}", username);
            return memberRepository.findByUsername(username)
                    .map(MemberEntity::getMemberId)
                    .orElse(null);
        }

        log.warn("extractMemberId: unknown principal type = {}", principal.getClass().getSimpleName());
        return null;
    }
}
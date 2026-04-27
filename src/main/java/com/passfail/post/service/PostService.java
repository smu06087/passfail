package com.passfail.post.service;

import com.passfail.entity.PostEntity;
import com.passfail.entity.PostLikeEntity;
import com.passfail.enums.PostCategory;
import com.passfail.post.dto.CommentResponseDTO;
import com.passfail.post.dto.PostCreateRequestDTO;
import com.passfail.post.dto.PostDetailResponseDTO;
import com.passfail.post.dto.PostListResponseDTO;
import com.passfail.post.dto.PostUpdateRequestDTO;
import com.passfail.post.exception.PostNotFoundException;
import com.passfail.post.exception.UnauthorizedPostAccessException;
import com.passfail.post.repository.CommentRepository;
import com.passfail.post.repository.PostLikeRepository;
import com.passfail.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CommentService commentService;

    private static final long VIEW_CACHE_TTL = 24;
    private static final TimeUnit VIEW_CACHE_UNIT = TimeUnit.HOURS;

    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> getPostList(PostCategory category, Pageable pageable) {
        Page<PostEntity> posts = (category == null)
                ? postRepository.findByIsDeletedFalseOrderByIsPinnedDescCreatedAtDesc(pageable)
                : postRepository.findByIsDeletedFalseAndCategoryOrderByIsPinnedDescCreatedAtDesc(category, pageable);

        return posts.map(PostListResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<PostListResponseDTO> searchPosts(
            PostCategory category, String keyword, Pageable pageable) {

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("createdAt"))
        );

        Page<PostEntity> posts = (category == null)
                ? postRepository.searchByKeywordOnly(keyword, sortedPageable)
                : postRepository.searchByKeywordAndCategory(category, keyword, sortedPageable);

        return posts.map(PostListResponseDTO::from);
    }
    
    
    @Transactional(readOnly = true)
    public PostDetailResponseDTO getPostDetail(Long postId, Long currentMemberId) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
        
        boolean isLiked = false;
        if (currentMemberId != null) {
            isLiked = postLikeRepository.existsByPostIdAndMemberId(postId, currentMemberId);
        }
        
        List<CommentResponseDTO> comments = commentService.getComments(postId);
        
        return PostDetailResponseDTO.from(post, currentMemberId, isLiked, comments);
    }

    @Transactional
    public Long createPost(PostCreateRequestDTO dto, Long memberId) {
        PostEntity post = PostEntity.builder()
                .memberId(memberId)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        return postRepository.save(post).getPostId();
    }

    @Transactional
    public void updatePost(Long postId, PostUpdateRequestDTO dto, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);

        post.setTitle(dto.getTitle());
        post.setContent(dto.getContent());
    }

    @Transactional
    public void deletePost(Long postId, Long currentMemberId) {
        PostEntity post = getActivePost(postId);
        validateAuthor(post, currentMemberId);

        post.setIsDeleted(true);
    }

    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        getActivePost(postId);

        boolean alreadyLiked = isLikedWithFallback(postId, memberId);

        if (alreadyLiked) {
            removeLike(postId, memberId);
            postLikeRepository.findByPostIdAndMemberId(postId, memberId)
                    .ifPresent(postLikeRepository::delete);
            postRepository.decrementLikeCount(postId);
            return false;
        } else {
            addLike(postId, memberId);
            postLikeRepository.save(PostLikeEntity.builder()
                    .postId(postId)
                    .memberId(memberId)
                    .build());
            postRepository.incrementLikeCount(postId);
            return true;
        }
    }

    @Transactional
    public void togglePin(Long postId) {
        PostEntity post = getActivePost(postId);
        post.setIsPinned(!post.getIsPinned());
    }

    private PostEntity getActivePost(Long postId) {
        return postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    private void validateAuthor(PostEntity post, Long currentMemberId) {
        if (!post.getMemberId().equals(currentMemberId)) {
            throw new UnauthorizedPostAccessException();
        }
    }

    private boolean isNewView(Long postId, Long memberId) {
        String key = "view:" + postId + ":" + memberId;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.FALSE.equals(exists)) {
            redisTemplate.opsForValue().set(key, "1", VIEW_CACHE_TTL, VIEW_CACHE_UNIT);
            return true;
        }
        return false;
    }

    private void addLike(Long postId, Long memberId) {
        String key = "like:" + postId;
        redisTemplate.opsForSet().add(key, memberId.toString());
    }

    private void removeLike(Long postId, Long memberId) {
        String key = "like:" + postId;
        redisTemplate.opsForSet().remove(key, memberId.toString());
    }

    private boolean isLikedWithFallback(Long postId, Long memberId) {
        String key = "like:" + postId;
        Boolean isInRedis = redisTemplate.opsForSet().isMember(key, memberId.toString());

        if (Boolean.FALSE.equals(isInRedis) || isInRedis == null) {
            boolean dbResult = postLikeRepository.existsByPostIdAndMemberId(postId, memberId);
            if (dbResult) {
                addLike(postId, memberId);
            }
            return dbResult;
        }

        return true;
    }
}
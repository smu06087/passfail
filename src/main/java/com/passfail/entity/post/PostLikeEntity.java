package com.passfail.entity.post;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import com.passfail.entity.member.MemberEntity;

import java.time.LocalDateTime;

@Entity
@Table(
	    name = "post_like",
	    uniqueConstraints = {
	        @UniqueConstraint(columnNames = {"post_id", "member_id"})
	    }
	)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;


	@Column(name = "post_id",nullable = false)
	private Long postId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", insertable = false, updatable = false)
	private PostEntity post;

	@Column(name = "member_id",nullable = false)
	private Long memberId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", insertable = false, updatable = false)
	private MemberEntity member;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime likedAt;
}

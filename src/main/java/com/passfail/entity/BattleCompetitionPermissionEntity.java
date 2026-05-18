package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "battle_competition_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleCompetitionPermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long permissionId;

    @Column(name = "member_id", nullable = false, unique = true)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", insertable = false, updatable = false)
    private MemberEntity member;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime grantedAt;
}

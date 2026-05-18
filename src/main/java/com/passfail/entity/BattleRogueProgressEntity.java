package com.passfail.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "battle_rogue_progress")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BattleRogueProgressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long progressId;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Builder.Default
    private Integer cumulativeScore = 0;

    private String currentNodeId;

    @Lob 
    @Column()
    private String visitedNodesJson; // ["0-0", "1-1"]

    @Lob 
    @Column()
    private String visitedPathsJson; // [["0-0", "1-1"]]
    
    @Builder.Default
    private boolean isFinished = false;
}

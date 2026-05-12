package com.passfail.admin.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberStatusDto {
    private long activeCount;
    private long inactiveCount;
    private long suspendedCount;
    private long withdrawnCount;
}

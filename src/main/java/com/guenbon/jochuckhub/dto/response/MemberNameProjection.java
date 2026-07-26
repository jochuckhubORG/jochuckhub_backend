package com.guenbon.jochuckhub.dto.response;

import lombok.Getter;

@Getter
public class MemberNameProjection {

    private final Long memberId;
    private final String memberName;

    public MemberNameProjection(Long memberId, String memberName) {
        this.memberId = memberId;
        this.memberName = memberName;
    }
}

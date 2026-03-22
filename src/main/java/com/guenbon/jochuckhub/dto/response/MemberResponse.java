package com.guenbon.jochuckhub.dto.response;

import com.guenbon.jochuckhub.entity.Member;
import com.guenbon.jochuckhub.entity.Position;
import lombok.Getter;

import java.util.Set;

@Getter
public class MemberResponse {

    private final Long id;
    private final String username;
    private final String name;
    private final Position mainPosition;
    private final Set<Position> subPositions;

    public MemberResponse(Member member) {
        this.id = member.getId();
        this.username = member.getUsername();
        this.name = member.getName();
        this.mainPosition = member.getMainPosition();
        this.subPositions = member.getSubPositions();
    }
}

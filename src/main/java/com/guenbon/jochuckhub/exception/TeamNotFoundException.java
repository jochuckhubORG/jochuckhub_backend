package com.guenbon.jochuckhub.exception;

import com.guenbon.jochuckhub.exception.errorcode.ErrorCode;

public class TeamNotFoundException extends RuntimeException {

    public TeamNotFoundException() {
        super(ErrorCode.TEAM_NOT_FOUND.getMessage());
    }
}

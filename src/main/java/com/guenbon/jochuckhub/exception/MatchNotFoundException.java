package com.guenbon.jochuckhub.exception;

import com.guenbon.jochuckhub.exception.errorcode.ErrorCode;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException() {
        super(ErrorCode.MATCH_NOT_FOUND.getMessage());
    }
}

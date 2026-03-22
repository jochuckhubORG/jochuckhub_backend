package com.guenbon.jochuckhub.exception;

import com.guenbon.jochuckhub.exception.errorcode.ErrorCode;

public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException() {
        super(ErrorCode.MEMBER_NOT_FOUND.getMessage());
    }
}

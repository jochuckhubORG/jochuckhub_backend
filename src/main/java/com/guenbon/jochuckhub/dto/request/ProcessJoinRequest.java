package com.guenbon.jochuckhub.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ProcessJoinRequest {

    @NotNull(message = "approved is required.")
    private Boolean approved;
}

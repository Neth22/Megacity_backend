package com.system.megacityCab.dto;

import lombok.Data;

@Data
public class CancellationRequest {

    private String bookingId;
    private String reason;

    
}

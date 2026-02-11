package com.gbm.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoutingStatsResponse {
    private long inboundRequests;
    private long outboundRequests;
    private long cacheHits;
    private long dedupedRequests;
    private int queueSize;
    private int cacheSize;
}

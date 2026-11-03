package com.bishop.offlinepay.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoryItem(String direction, String otherName, BigDecimal amount,
                          BigDecimal fee, String status, LocalDateTime timestamp) {}
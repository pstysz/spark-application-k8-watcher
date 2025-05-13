package com.stysz.model;

import org.springframework.lang.Nullable;

public record JobStatusUpdate(@Nullable String sparkApplicationId, String name, String status, String updatedAt) {}

package com.avatarstore.dto;

import java.util.List;

public record CheckoutRequest(List<Long> versionIds) {}

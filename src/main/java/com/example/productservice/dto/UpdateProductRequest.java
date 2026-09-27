package com.example.productservice.dto;

import java.math.BigDecimal;

public record UpdateProductRequest(
    String name,
    String description,
    BigDecimal price,
    String imageUrl
) {}

package com.example.productservice.dto;

import com.example.productservice.model.Product;
import java.io.Serializable;
import java.math.BigDecimal;

public record ProductResponse(
    Long id,
    String name,
    String description,
    BigDecimal price,
    String imageUrl
) implements Serializable {
    public static ProductResponse from(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getImageUrl()
        );
    }
}

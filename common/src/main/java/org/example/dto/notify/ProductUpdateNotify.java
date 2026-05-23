package org.example.dto.notify;

import org.example.dto.response.ProductResponse;

public class ProductUpdateNotify {
    private ProductResponse product;

    public ProductUpdateNotify() {}

    public ProductUpdateNotify(ProductResponse product) {
        this.product = product;
    }

    public ProductResponse getProduct() { return product; }
    public void setProduct(ProductResponse product) { this.product = product; }
}

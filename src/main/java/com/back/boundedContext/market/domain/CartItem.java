package com.back.boundedContext.market.domain;

import com.back.global.entity.BaseIdAndTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Table(name = "MARKET_CART_ITEM")
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class CartItem extends BaseIdAndTime {
    @ManyToOne(fetch = LAZY)
    private Cart cart;
    @ManyToOne(fetch = LAZY)
    private Product product;
}
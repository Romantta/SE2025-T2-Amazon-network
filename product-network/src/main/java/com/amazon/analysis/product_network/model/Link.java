package com.amazon.analysis.product_network.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** 商品共购关系边模型 表示两个商品之间的共购关系 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    /** 源节点ID 表示关系的起始商品 */
    private Integer source;

    /** 目标节点ID 表示关系的目标商品 */
    private Integer target;
}
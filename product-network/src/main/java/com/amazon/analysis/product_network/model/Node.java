package com.amazon.analysis.product_network.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/** 商品节点数据模型 对应JSON中的节点对象，表示一个商品 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Node {
    /**  节点唯一标识ID */
    private Integer id;

    /** 亚马逊商品ASIN编号 */
    private String asin;

    /** 商品标题 */
    private String title;

    /** 商品类别 */
    private String group;

    /**节点度数 表示该商品与其他商品的连接数量 */
    private Integer degree;
}
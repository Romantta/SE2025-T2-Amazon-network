package com.amazon.analysis.product_network.model;

import lombok.Data;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 完整的商品共购网络数据模型
 * 包含所有节点、边和元数据信息
 * 对应清洗后的JSON文件结构
 */
@Data
public class NetworkData {
    /** 节点列表 包含所有商品节点的详细信息 */
    private List<Node> nodes;

    /** 边列表 包含所有商品间的共购关系 */
    private List<Link> links;

    /** 元数据信息 包含网络数据的基本统计信息 */
    private Metadata metadata;

    /** 元数据内部类 描述网络数据的整体信息 */
    @Data
    public static class Metadata {
        /** 网络中的总节点数 */
        @JsonProperty("total_nodes")
        private Integer totalNodes;

        /** 网络中的总边数 */
        @JsonProperty("total_edges")
        private Integer totalEdges;

        /** 数据清洗时间 ISO格式的时间戳 */
        @JsonProperty("cleaned_at")
        private String cleanedAt;
    }
}
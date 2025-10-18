package com.amazon.analysis.product_network.service;

import com.amazon.analysis.product_network.model.NetworkData;
import com.amazon.analysis.product_network.model.Node;
import com.amazon.analysis.product_network.model.Link;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/** 网络数据服务类 负责加载、处理和提供商品共购关系数据 使用@PostConstruct在启动时自动加载数据 */
@Slf4j
@Service
public class NetworkDataService {

    // 存储完整的网络数据
    private NetworkData networkData;
    // 节点ID到节点对象的映射，用于快速查找
    private Map<Integer, Node> nodeMap;

    /** 初始化方法
     * 从JSON文件加载网络数据到内存中
     * @throws RuntimeException 如果数据加载失败
     */
    @PostConstruct
    public void init() {
        log.info("开始加载商品共购网络数据...");
        try {
            // 创建JSON解析器
            ObjectMapper objectMapper = new ObjectMapper();

            // 从classpath的data目录加载JSON文件
            ClassPathResource resource = new ClassPathResource("data/amazon_co_purchase_network.json");
            InputStream inputStream = resource.getInputStream();

            // 解析JSON到Java对象
            this.networkData = objectMapper.readValue(inputStream, NetworkData.class);

            // 构建节点映射表，便于通过ID快速查找节点
            this.nodeMap = networkData.getNodes().stream()
                    .collect(Collectors.toMap(Node::getId, node -> node));

            log.info("网络数据加载成功: {} 个节点, {} 条边",
                    networkData.getMetadata().getTotalNodes(),
                    networkData.getMetadata().getTotalEdges());

        } catch (IOException e) {
            log.error("加载网络数据失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法加载网络数据文件", e);
        }
    }

    /** 获取完整的网络数据
     *  @return NetworkData 包含所有节点和边的完整网络数据 */
    public NetworkData getFullNetworkData() {
        log.debug("获取完整网络数据");
        return this.networkData;
    }

    /** 根据节点ID获取特定的节点信息
     * @param nodeId 节点ID
     * @return Node 节点对象，如果不存在返回null
     */
    public Node getNodeById(Integer nodeId) {
        log.debug("根据ID查询节点: {}", nodeId);
        return nodeMap.get(nodeId);
    }

    /** 获取网络中所有不同的商品类别
     *  @return List<String> 去重后的商品类别列表
     */
    public List<String> getAllGroups() {
        log.debug("获取所有商品类别");
        return networkData.getNodes().stream()
                .map(Node::getGroup)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** 根据商品类别过滤节点
     * @param group 商品类别（如"Book", "Music"等）
     * @return List<Node> 该类别下的所有节点
     */
    public List<Node> getNodesByGroup(String group) {
        log.debug("根据类别过滤节点: {}", group);
        return networkData.getNodes().stream()
                .filter(node -> group.equals(node.getGroup()))
                .collect(Collectors.toList());
    }

    /** 获取连接度最高的节点 即最受欢迎的商品
     * @param limit 返回节点的数量限制
     * @return List<Node> 按连接度降序排列的节点列表
     */
    public List<Node> getHighlyConnectedNodes(Integer limit) {
        log.debug("获取高度连接节点，限制: {}", limit);
        return networkData.getNodes().stream()
                .sorted((n1, n2) -> n2.getDegree().compareTo(n1.getDegree()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /** 获取与指定节点直接相连的所有边
     * @param nodeId 节点ID
     * @return List<Link> 与该节点相关的所有边
     */
    public List<Link> getLinksByNodeId(Integer nodeId) {
        log.debug("获取节点 {} 的相关边", nodeId);
        return networkData.getLinks().stream()
                .filter(link -> link.getSource().equals(nodeId) || link.getTarget().equals(nodeId))
                .collect(Collectors.toList());
    }

    /** 获取指定节点的所有邻居节点（一度关系）
     * @param nodeId 中心节点ID
     * @return List<Node> 邻居节点列表
     */
    public List<Node> getNeighborNodes(Integer nodeId) {
        log.debug("获取节点 {} 的邻居节点", nodeId);
        Set<Integer> neighborIds = new HashSet<>();

        // 遍历所有边，找到与指定节点相连的其他节点
        networkData.getLinks().stream()
                .filter(link -> link.getSource().equals(nodeId) || link.getTarget().equals(nodeId))
                .forEach(link -> {
                    if (link.getSource().equals(nodeId)) {
                        neighborIds.add(link.getTarget());
                    } else {
                        neighborIds.add(link.getSource());
                    }
                });

        // 将邻居节点ID转换为节点对象
        return neighborIds.stream()
                .map(this::getNodeById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /** 获取网络的整体统计信息
     * @return Map<String, Object> 包含各种统计指标的映射
     */
    public Map<String, Object> getNetworkStatistics() {
        log.debug("计算网络统计信息");
        Map<String, Object> stats = new HashMap<>();

        // 基本统计信息
        stats.put("totalNodes", networkData.getMetadata().getTotalNodes());
        stats.put("totalEdges", networkData.getMetadata().getTotalEdges());
        stats.put("cleanedAt", networkData.getMetadata().getCleanedAt());

        // 商品类别分布统计
        Map<String, Long> groupDistribution = networkData.getNodes().stream()
                .collect(Collectors.groupingBy(Node::getGroup, Collectors.counting()));
        stats.put("groupDistribution", groupDistribution);

        // 节点度数统计
        Double averageDegree = networkData.getNodes().stream()
                .mapToInt(Node::getDegree)
                .average()
                .orElse(0.0);
        stats.put("averageDegree", Math.round(averageDegree * 100.0) / 100.0); // 保留两位小数

        Integer maxDegree = networkData.getNodes().stream()
                .mapToInt(Node::getDegree)
                .max()
                .orElse(0);
        stats.put("maxDegree", maxDegree);

        Integer minDegree = networkData.getNodes().stream()
                .mapToInt(Node::getDegree)
                .min()
                .orElse(0);
        stats.put("minDegree", minDegree);

        log.debug("统计计算完成: 平均度={}, 最大度={}", averageDegree, maxDegree);
        return stats;
    }

    /** 搜索包含关键字的商品节点
     * @param keyword 搜索关键词
     * @return List<Node> 匹配的商品节点列表
     */
    public List<Node> searchNodes(String keyword) {
        log.debug("搜索商品节点，关键词: {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String lowerKeyword = keyword.toLowerCase();
        return networkData.getNodes().stream()
                .filter(node ->
                        (node.getTitle() != null && node.getTitle().toLowerCase().contains(lowerKeyword)) ||
                                (node.getGroup() != null && node.getGroup().toLowerCase().contains(lowerKeyword)) ||
                                (node.getAsin() != null && node.getAsin().toLowerCase().contains(lowerKeyword))
                )
                .collect(Collectors.toList());
    }
}
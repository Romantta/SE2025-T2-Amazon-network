import re
import pandas as pd
import json

class AmazonDataCleaner:
    def __init__(self, max_nodes=100):
        self.max_nodes = max_nodes
        self.nodes_data = []
        self.edges_data = []
    
    def clean_amazon_meta(self, input_file):
        """
        清洗Amazon元数据文件,只提取核心信息
        """
        print("开始清洗节点数据...")
        nodes = []
        current_node = {}
        node_count = 0
        
        with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
            for line in f:
                line = line.strip()
                
                # 检测新节点开始
                if line.startswith('Id:'):
                    # 保存前一个节点（如果有）
                    if current_node and 'id' in current_node:
                        # 只保留核心字段
                        core_node = {
                            'id': current_node.get('id'),
                            'asin': current_node.get('asin', ''),
                            'title': current_node.get('title', ''),
                            'group': current_node.get('group', 'Unknown')
                        }
                        nodes.append(core_node)
                        node_count += 1
                        if node_count >= self.max_nodes:
                            break
                    
                    # 开始新节点
                    current_node = {'id': int(line.split()[1])}
                
                # 提取ASIN
                elif line.startswith('ASIN:'):
                    current_node['asin'] = line.split()[1]
                
                # 提取标题
                elif line.startswith('title:'):
                    title = line[6:].strip()
                    # 清理标题中的特殊字符
                    title = re.sub(r'[^\x00-\x7F]+', ' ', title)
                    current_node['title'] = title[:80]  # 限制标题长度
                
                # 提取商品类别
                elif line.startswith('group:'):
                    current_node['group'] = line.split()[1]
        
        # 添加最后一个节点
        if current_node and 'id' in current_node and node_count < self.max_nodes:
            core_node = {
                'id': current_node.get('id'),
                'asin': current_node.get('asin', ''),
                'title': current_node.get('title', ''),
                'group': current_node.get('group', 'Unknown')
            }
            nodes.append(core_node)
        
        self.nodes_data = nodes
        print(f"成功清洗 {len(self.nodes_data)} 个节点")
        return self.nodes_data
    
    def clean_amazon_edges(self, input_file):
        """清洗Amazon边数据,只保留与清洗节点相关的边"""
        print("开始清洗边数据...")

        if not self.nodes_data:
            raise ValueError("请先清洗节点数据")
        
        # 获取有效的节点ID集合
        valid_node_ids = {node['id'] for node in self.nodes_data}
        print(f"有效节点ID数量: {len(valid_node_ids)}")
        
        edges = []
        edge_count = 0
        
        with open(input_file, 'r') as f:
            for line in f:
                parts = line.strip().split()
                if len(parts) >= 2:
                    try:
                        source = int(parts[0])
                        target = int(parts[1])
                        
                        # 只保留两个节点都在有效节点集合中的边
                        if source in valid_node_ids and target in valid_node_ids:
                            edges.append({
                                'source': source,
                                'target': target
                            })
                            edge_count += 1
                            
                            # 限制边数量以避免过度复杂
                            if edge_count >= 800:
                                break
                    
                    except ValueError:
                        continue
        
        self.edges_data = edges
        
        # 计算每个节点的度（连接数）
        node_degrees = {}
        for edge in self.edges_data:
            source = edge['source']
            target = edge['target']
            node_degrees[source] = node_degrees.get(source, 0) + 1
            node_degrees[target] = node_degrees.get(target, 0) + 1
        
        # 将度信息添加到节点数据中
        for node in self.nodes_data:
            node['degree'] = node_degrees.get(node['id'], 0)
        
        print(f"✅ 成功清洗 {len(self.edges_data)} 条边")
        return self.edges_data
    
    def save_as_json(self, output_file='cleaned_amazon_network.json'):
        """
        保存清洗后的数据为单一JSON文件
        """
        if not self.nodes_data or not self.edges_data:
            raise ValueError("没有可保存的数据")
        
        # 构建完整的网络数据
        network_data = {
            "nodes": self.nodes_data,
            "links": self.edges_data,
            "metadata": {
                "total_nodes": len(self.nodes_data),
                "total_edges": len(self.edges_data),
                "cleaned_at": pd.Timestamp.now().isoformat()
            }
        }
        
        # 保存为JSON文件
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(network_data, f, indent=2, ensure_ascii=False)
        
        print(f"数据已保存至: {output_file}")
        
        # 显示简要统计
        groups = {}
        for node in self.nodes_data:
            group = node['group']
            groups[group] = groups.get(group, 0) + 1
        
        print(f"\n数据统计:")
        print(f"  节点总数: {len(self.nodes_data)}")
        print(f"  边总数: {len(self.edges_data)}")
        print(f"  商品类别分布: {groups}")
        
        avg_degree = sum(node['degree'] for node in self.nodes_data) / len(self.nodes_data)
        print(f"  平均连接度: {avg_degree:.2f}")
    
    def get_data_preview(self):
        """获取数据预览"""
        if not self.nodes_data:
            return "数据未清洗"
        
        print("\n数据预览:")
        print("前3个节点:")
        for i, node in enumerate(self.nodes_data[:3]):
            print(f"  {i+1}. ID:{node['id']}, 标题:{node['title'][:30]}..., 类别:{node['group']}, 度:{node['degree']}")
        
        print("\n前3条边:")
        for i, edge in enumerate(self.edges_data[:3]):
            print(f"  {i+1}. {edge['source']} → {edge['target']}")

def main():
    """主函数"""
    cleaner = AmazonDataCleaner(max_nodes=100)
    
    try:
        # 清洗节点数据
        nodes = cleaner.clean_amazon_meta('amazon-meta.txt')
        # 清洗边数据
        edges = cleaner.clean_amazon_edges('amazon0302.txt')
        # 保存为JSON文件
        cleaner.save_as_json('amazon_co_purchase_network.json')
        # 显示数据预览
        cleaner.get_data_preview()
        
        print(f"\n数据清洗完成,JSON文件已生成。")
        
    except FileNotFoundError as e:
        print(f"文件未找到: {e}")
        print("请确保以下文件存在于当前目录:")
        print("  - amazon-meta.txt")
        print("  - amazon0302.txt")
    except Exception as e:
        print(f"清洗过程中出现错误: {e}")

if __name__ == "__main__":
    main()
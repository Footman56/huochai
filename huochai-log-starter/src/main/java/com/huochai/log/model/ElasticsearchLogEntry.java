package com.huochai.log.model;

import com.huochai.log.context.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Elasticsearch 操作日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ElasticsearchLogEntry extends LogEntry {

    /**
     * 操作类型 (index/search/delete/update/bulk)
     */
    private String operation;

    /**
     * 索引名
     */
    private String index;

    /**
     * 文档ID
     */
    private String docId;

    /**
     * 查询语句
     */
    private String query;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态
     */
    private Integer responseStatus;

    /**
     * 命中数量
     */
    private Integer hitCount;

    /**
     * ES 集群节点
     */
    private String node;
}
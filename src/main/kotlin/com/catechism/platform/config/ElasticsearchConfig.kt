package com.catechism.platform.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories

/**
 * Elasticsearch configuration — only activated when search.enabled=true in
 * application.yml (or as an environment variable SEARCH_ENABLED=true).
 *
 * This means the entire search subsystem — client, repositories, and service —
 * is completely absent from the Spring context when Elasticsearch is not running.
 * The app starts normally and all other features work without it.
 *
 * To enable search:
 *   1. Start Elasticsearch:
 *      docker run -p 9200:9200 -e "discovery.type=single-node" elasticsearch:8.10.4
 *   2. Set search.enabled=true in application.yml (or -Dsearch.enabled=true at runtime)
 *   3. Call the reindexAll GraphQL mutation to populate the index
 */
@Configuration
@ConditionalOnProperty(name = ["search.enabled"], havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = ["com.catechism.platform.search"])
class ElasticsearchConfig : ElasticsearchConfiguration() {

    override fun clientConfiguration(): ClientConfiguration {
        return ClientConfiguration.builder()
            .connectedTo("localhost:9200")
            .withConnectTimeout(java.time.Duration.ofSeconds(5))
            .withSocketTimeout(java.time.Duration.ofSeconds(30))
            .build()
    }
}
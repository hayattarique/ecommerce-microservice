package org.ecommerce.auth.service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Connection POOL settings only. All values are in SECONDS.
 *
 * Connect and read timeouts are DELIBERATELY absent here - they come from Boot's own
 * properties:
 *
 *   spring.http.clients.connect-timeout            -> default for every group
 *   spring.http.clients.read-timeout               -> default for every group
 *   spring.http.serviceclient.GROUP.read-timeout   -> override for one service only
 *
 * Reason: Boot supports a per-group override, we cannot. If the timeouts lived here too,
 * a single value would stick to every group and that third line would never work. Boot
 * does not model the pool, so the pool is ours.
 */
@ConfigurationProperties(prefix = "rest.client.config")
@Getter
@Setter
public class RestClientProperties {

    private int maxConnection;
    private int maxConnectionPerRoute;
    private int connectionRequestTimeout;
    private int connectionTimeToLive;
    private int validateAfterInactive;
    private int idleEvictionThreshold;

}

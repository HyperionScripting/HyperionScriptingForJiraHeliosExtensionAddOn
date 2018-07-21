package hyperion.jira.extension.manager;

import com.atlassian.cache.*;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Strings;
import hyperion.jira.extension.ao.Repository;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import hyperion.jira.extension.exceptions.HTTPEndpointNotFoundException;
import hyperion.jira.extension.servlet.models.HTTPEndpointCachedEntry;
import hyperion.jira.interop.managers.JSONManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;
import java.util.concurrent.TimeUnit;

@Component
public class HeliosCacheManager {
    private static final Logger logger = LoggerFactory.getLogger(HeliosCacheManager.class);
    private final Repository repository;
    private final JSONManager jsonManager;
    private final Cache<String, HTTPEndpointCachedEntry> httpEndpointsCache;

    @Autowired
    public HeliosCacheManager(@ComponentImport CacheManager cacheManager, Repository repository, @ComponentImport JSONManager jsonManager) {
        this.repository = repository;
        this.jsonManager = jsonManager;
        CacheSettings remoteCacheSettings = new CacheSettingsBuilder().remote().replicateAsynchronously().replicateViaInvalidation().maxEntries(Integer.MAX_VALUE).expireAfterAccess(Integer.MAX_VALUE, TimeUnit.HOURS).expireAfterWrite(Integer.MAX_VALUE, TimeUnit.HOURS).build();
        httpEndpointsCache = cacheManager.getCache("helios-http-endpoints", getHTTPEndpointCacheLoader(), remoteCacheSettings);
    }

    public Cache<String, HTTPEndpointCachedEntry> getHttpEndpointsCache() {
        return httpEndpointsCache;
    }

    private CacheLoader<String, HTTPEndpointCachedEntry> getHTTPEndpointCacheLoader() {
        return key -> {
            logger.info("[Hyperion] Loading HTTP Endpoint into cache for endpoint name: " + key);
            AOHTTPEndpoint endpoint = repository.getHttpEndpoint(key);
            if (endpoint == null) {
                throw new HTTPEndpointNotFoundException("HTTP Endpoint not found: "+key);
            } else {
                String jsonConfig = endpoint.getJSONConfiguration();
                Object serializedJsonConfig = new Object();
                if (!Strings.isNullOrEmpty(jsonConfig)) {
                    try {
                        serializedJsonConfig = jsonManager.fromJSONUsingJS(jsonConfig);
                    } catch (NoSuchMethodException | ScriptException e) {
                        logger.error("[Hyperion] Failed to serialize JSON config for HTTP Endpoint: " + key);
                    }
                }
                endpoint.setJSONConfiguration(null);
                return new HTTPEndpointCachedEntry(endpoint, serializedJsonConfig);
            }
        };
    }
}

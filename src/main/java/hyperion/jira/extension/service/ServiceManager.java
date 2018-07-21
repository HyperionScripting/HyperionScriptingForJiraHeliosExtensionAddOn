package hyperion.jira.extension.service;

import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import hyperion.jira.extension.ao.Repository;
import hyperion.jira.extension.manager.HeliosCacheManager;
import hyperion.jira.interop.managers.ScriptManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
/**
 * For some reason injecting services listed here is not possible to be injected to certain classes, such as WebActions, therefor we hold references here.
 */
public class ServiceManager {
    private static Repository repository;
    private static HeliosCacheManager cacheManager;

    @Autowired
    public ServiceManager(Repository repository, HeliosCacheManager cacheManager) {
        ServiceManager.repository = repository;
        ServiceManager.cacheManager = cacheManager;
    }

    public static Repository getRepository() {
        return repository;
    }

    public static HeliosCacheManager getCacheManager() {
        return cacheManager;
    }
}
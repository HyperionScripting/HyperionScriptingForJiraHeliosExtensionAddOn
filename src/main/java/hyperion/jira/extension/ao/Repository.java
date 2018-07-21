package hyperion.jira.extension.ao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import net.java.ao.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Scanned
@Component
@ExportAsService
public class Repository {
    private final ActiveObjects activeObjects;

    @Autowired
    Repository(@ComponentImport ActiveObjects activeObjects) {
        this.activeObjects = activeObjects;
    }

    public AOHTTPEndpoint[] getAllHttpEndpoints() {
        return activeObjects.find(AOHTTPEndpoint.class, Query.select().order("ID"));
    }

    public AOHTTPEndpoint[] getHttpEndpoints(String resourceID) {
        return activeObjects.find(AOHTTPEndpoint.class, Query.select().where("RESOURCE_ID = ?", resourceID));
    }

    public AOHTTPEndpoint getHttpEndpoint(String endpointName) {
        return Arrays.stream(activeObjects.find(AOHTTPEndpoint.class, Query.select().where("ENDPOINT_NAME = ?", endpointName))).findFirst().orElse(null);
    }

    public AOHTTPEndpoint[] getHttpEndpointsByName(String endpointName) {
        return activeObjects.find(AOHTTPEndpoint.class, Query.select().where("ENDPOINT_NAME = ?", endpointName));
    }

    public AOHTTPEndpoint getHttpEndpoint(int id) {
        return Arrays.stream(activeObjects.find(AOHTTPEndpoint.class, Query.select().where("ID = ?", id))).findFirst().orElse(null);
    }

    public int addHttpEndpoint(String endpointName, String description, String resourceId, String jsonConfig, boolean allowAnonymous, boolean methodGet, boolean methodHead, boolean methodPost, boolean methodPut, boolean methodDelete, boolean methodOptions, boolean methodTrace, boolean methodPatch) {
        AOHTTPEndpoint aoHttpEndpoint = activeObjects.create(AOHTTPEndpoint.class);
        aoHttpEndpoint.setEndpointName(endpointName);
        aoHttpEndpoint.setDescription(description);
        aoHttpEndpoint.setResourceID(resourceId);
        aoHttpEndpoint.setJSONConfiguration(jsonConfig);
        aoHttpEndpoint.setAllowAnonymous(allowAnonymous);
        aoHttpEndpoint.setMethodGet(methodGet);
        aoHttpEndpoint.setMethodHead(methodHead);
        aoHttpEndpoint.setMethodPost(methodPost);
        aoHttpEndpoint.setMethodPut(methodPut);
        aoHttpEndpoint.setMethodDelete(methodDelete);
        aoHttpEndpoint.setMethodOptions(methodOptions);
        aoHttpEndpoint.setMethodTrace(methodTrace);
        aoHttpEndpoint.setMethodPatch(methodPatch);
        aoHttpEndpoint.save();
        return aoHttpEndpoint.getID();
    }

    public void deleteHttpEndpoint(AOHTTPEndpoint httpEndpoint) {
        activeObjects.delete(httpEndpoint);
    }
}

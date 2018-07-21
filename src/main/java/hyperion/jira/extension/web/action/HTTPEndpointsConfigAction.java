package hyperion.jira.extension.web.action;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.google.common.base.Strings;
import hyperion.jira.extension.ao.Repository;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import hyperion.jira.extension.manager.HeliosCacheManager;
import hyperion.jira.extension.service.ServiceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HTTPEndpointsConfigAction extends JiraWebActionSupport {
    private static final Logger logger = LoggerFactory.getLogger(HTTPEndpointsConfigAction.class);
    private final Repository repository;
    private final HeliosCacheManager cacheManager;

    public HTTPEndpointsConfigAction() {
        this.repository = ServiceManager.getRepository();
        this.cacheManager = ServiceManager.getCacheManager();
    }

    @Override
    @RequiresXsrfCheck
    public String execute() {
        String action = getHttpRequest().getParameter("action");
        if (!Strings.isNullOrEmpty(action) && action.equals("delete")) {
            int httpEndpointId = 0;
            try {
                httpEndpointId = Integer.parseInt(getHttpRequest().getParameter("httpEndpointId"));
                AOHTTPEndpoint endpoint = repository.getHttpEndpoint(httpEndpointId);
                if (endpoint != null) {
                    repository.deleteHttpEndpoint(endpoint);
                    cacheManager.getHttpEndpointsCache().remove(endpoint.getEndpointName());
                }
            } catch (Exception e) {
                logger.error("[Helios] Failed to delete HTTP Endpoint - HTTP Endpoint ID: " + httpEndpointId, e);
            }
        }
        return INPUT;
    }

    public List<AOHTTPEndpoint> getHttpEndpoints() {
        List<AOHTTPEndpoint> endpoints = Arrays.asList(repository.getAllHttpEndpoints());
        Collections.reverse(endpoints);
        return endpoints;
    }

    public String getScript(AOHTTPEndpoint httpEndpoint) {
        String[] tokens = httpEndpoint.getResourceID().split("\\.");
        if (tokens.length == 5) {
            return "[Built-In] "+tokens[3];
        } else if (tokens.length == 4) {
            return tokens[3];
        } else {
            return httpEndpoint.getResourceID();
        }
    }

    public String getAllowedMethods(AOHTTPEndpoint httpEndpoint) {
        List<String> methods = new LinkedList<>();
        if (httpEndpoint.isMethodGet()) {
            methods.add("GET");
        }
        if (httpEndpoint.isMethodHead()) {
            methods.add("HEAD");
        }
        if (httpEndpoint.isMethodPost()) {
            methods.add("POST");
        }
        if (httpEndpoint.isMethodPut()) {
            methods.add("PUT");
        }
        if (httpEndpoint.isMethodDelete()) {
            methods.add("DELETE");
        }
        if (httpEndpoint.isMethodOptions()) {
            methods.add("OPTIONS");
        }
        if (httpEndpoint.isMethodTrace()) {
            methods.add("TRACE");
        }
        if (httpEndpoint.isMethodPatch()) {
            methods.add("PATCH");
        }
        return String.join(", ", methods);
    }

    public String getURL(AOHTTPEndpoint httpEndpoint) {
        return ComponentAccessor.getOSGiComponentInstanceOfType(JiraBaseUrls.class).baseUrl()+"/plugins/servlet/helios/"+httpEndpoint.getEndpointName();
    }

    public boolean httpEndpointSaved() {
        return getHttpRequest().getParameter("saved") != null;
    }
}

package hyperion.jira.extension.web.action;

import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.base.Strings;
import hyperion.jira.extension.ao.Repository;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import hyperion.jira.extension.constant.HeliosCombinedResourceID;
import hyperion.jira.extension.manager.HeliosCacheManager;
import hyperion.jira.extension.service.ServiceManager;
import hyperion.jira.interop.managers.JSONManager;
import hyperion.jira.interop.managers.ResourceManager;
import hyperion.jira.interop.models.ConfigurableResource;
import hyperion.jira.interop.models.HyperionResource;
import hyperion.jira.interop.models.ResourceTemplate;
import hyperion.jira.interop.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.script.ScriptException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EditHTTPEndpointAction extends JiraWebActionSupport {
    private static final Logger logger = LoggerFactory.getLogger(EditHTTPEndpointAction.class);
    private final Repository repository;
    private final ResourceManager resourceManager;
    private final JSONManager jsonManager;
    private final HeliosCacheManager cacheManager;
    private int httpEndpointId = 0;
    private String name = "";
    private String description = "";
    private String selectedResource;
    private String allowAnonymous;
    private String methodGet;
    private String methodHead;
    private String methodPost;
    private String methodPut;
    private String methodDelete;
    private String methodOptions;
    private String methodTrace;
    private String methodPatch;
    private String jsonConfig = "";

    @Autowired
    public EditHTTPEndpointAction(@ComponentImport ResourceManager resourceManager, @ComponentImport JSONManager jsonManager) {
        this.resourceManager = resourceManager;
        this.jsonManager = jsonManager;
        this.repository = ServiceManager.getRepository();
        this.cacheManager = ServiceManager.getCacheManager();
    }

    @Override
    @RequiresXsrfCheck
    public String execute() {
        try {
            if (getHttpRequest().getParameter("json-config") != null) {
                if (getHttpRequest().getParameter("httpEndpointId") != null) {
                    httpEndpointId = Integer.parseInt(getHttpRequest().getParameter("httpEndpointId"));
                }
                name = getHttpRequest().getParameter("name");
                description = getHttpRequest().getParameter("description");
                selectedResource = getHttpRequest().getParameter("selected-resource");
                jsonConfig = getHttpRequest().getParameter("json-config");
                allowAnonymous = getHttpRequest().getParameter("allow-anonymous");
                methodGet = getHttpRequest().getParameter("method-get");
                methodHead = getHttpRequest().getParameter("method-head");
                methodPost = getHttpRequest().getParameter("method-post");
                methodPut = getHttpRequest().getParameter("method-put");
                methodDelete = getHttpRequest().getParameter("method-delete");
                methodOptions = getHttpRequest().getParameter("method-options");
                methodTrace = getHttpRequest().getParameter("method-trace");
                methodPatch = getHttpRequest().getParameter("method-patch");
                if (selectedResource == null) {
                    addErrorMessage("Please select valid Hyperion script.");
                    return INPUT;
                }
                HyperionResource aoResource = resourceManager.getResource(selectedResource, HeliosCombinedResourceID.HttpEndpoint.DEFAULT);
                ResourceTemplate template = resourceManager.getResourceTemplates().stream().filter(rt -> rt.getId().equals(selectedResource.replaceAll(".Runnable", "")) && rt.getResourceType().equals(HeliosCombinedResourceID.HttpEndpoint.DEFAULT)).findFirst().orElse(null);
                if (aoResource == null && template == null) {
                    addErrorMessage("Script with such name not found: " + selectedResource);
                    return INPUT;
                }
                if (Strings.isNullOrEmpty(name) || name.trim().length() == 0) {
                    addErrorMessage("Name is required");
                    return INPUT;
                }
                if (name.trim().length() > 50) {
                    addErrorMessage("Name length must not exceed 50 characters");
                    return INPUT;
                }
                if (!name.trim().matches("^[a-zA-Z0-9]*$")) {
                    addErrorMessage("Invalid name, only letters and numbers are allowed");
                    return INPUT;
                }
                if (httpEndpointId == 0) {
                    if (repository.getHttpEndpointsByName(name.toLowerCase().trim()).length > 0) {
                        addErrorMessage("Endpoint with such name already exists");
                        return INPUT;
                    }
                } else {
                    if (Arrays.stream(repository.getHttpEndpointsByName(name.toLowerCase().trim())).anyMatch(e -> e.getID() != httpEndpointId)) {
                        addErrorMessage("Endpoint with such name already exists");
                        return INPUT;
                    }
                }
                if (!Strings.isNullOrEmpty(jsonConfig)) {
                    try {
                        jsonManager.fromJSONUsingJS(jsonConfig);
                    } catch (ScriptException | NoSuchMethodException e) {
                        addErrorMessage("Error while parsing JSON config: " + e.getMessage());
                        return INPUT;
                    }
                }

                if (httpEndpointId > 0) {
                    AOHTTPEndpoint endpoint = repository.getHttpEndpoint(httpEndpointId);
                    if (endpoint != null) {
                        repository.deleteHttpEndpoint(endpoint);
                        cacheManager.getHttpEndpointsCache().remove(endpoint.getEndpointName());
                    }
                }
                repository.addHttpEndpoint(name.toLowerCase().trim(), description, HeliosCombinedResourceID.HttpEndpoint.DEFAULT+"."+selectedResource, jsonConfig, allowAnonymous != null,methodGet != null, methodHead != null, methodPost != null, methodPut != null, methodDelete != null, methodOptions != null, methodTrace != null, methodPatch != null);
                cacheManager.getHttpEndpointsCache().remove(name.toLowerCase().trim());
                return getRedirect("HeliosHttpEndpointsConfigAction.jspa?saved=true");
            } else if (getHttpRequest().getParameter("httpEndpointId") != null) {
                httpEndpointId = Integer.parseInt(getHttpRequest().getParameter("httpEndpointId"));
                if (httpEndpointId > 0) {
                    AOHTTPEndpoint httpEndpoint = repository.getHttpEndpoint(httpEndpointId);
                    name = httpEndpoint.getEndpointName();
                    description = httpEndpoint.getDescription();
                    selectedResource = StringUtils.getResourceName(httpEndpoint.getResourceID());
                    jsonConfig = httpEndpoint.getJSONConfiguration();
                    allowAnonymous = httpEndpoint.isAllowAnonymous() ? "true" : null;
                    methodGet = httpEndpoint.isMethodGet() ? "true" : null;
                    methodHead = httpEndpoint.isMethodHead() ? "true" : null;
                    methodPost = httpEndpoint.isMethodPost() ? "true" : null;
                    methodPut = httpEndpoint.isMethodPut() ? "true" : null;
                    methodDelete = httpEndpoint.isMethodDelete() ? "true" : null;
                    methodOptions = httpEndpoint.isMethodOptions() ? "true" : null;
                    methodTrace = httpEndpoint.isMethodTrace() ? "true" : null;
                    methodPatch = httpEndpoint.isMethodPatch() ? "true" : null;
                }
            }
        } catch (Exception e) {
            addErrorMessage("Error while saving HTTP Endpoint, please check the logs.");
            logger.error("[Helios] Error while saving HTTP Endpoint configuration", e);
        }
        return INPUT;
    }

    public List<ConfigurableResource> getResources() {
        List<ConfigurableResource> resources = resourceManager.getResourcesForType(HeliosCombinedResourceID.HttpEndpoint.DEFAULT).stream().map(r -> new ConfigurableResource(r.getName(), r.getResourceType())).collect(Collectors.toList());
        for (ResourceTemplate template : resourceManager.getResourceTemplates()) {
            if (template.getResourceType().equalsIgnoreCase(HeliosCombinedResourceID.HttpEndpoint.DEFAULT) && (template.getAccessLevel() == ResourceTemplate.AccessLevel.RUNNABLE_ONLY || template.getAccessLevel() == ResourceTemplate.AccessLevel.EDITABLE_AND_RUNNABLE)) {
                resources.add(new ConfigurableResource(template.getId(), template.getResourceType(), true));
            }
        }
        return resources;
    }

    public int getHttpEndpointId() {
        return httpEndpointId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getSelectedResource() {
        return selectedResource;
    }

    public String getJSONConfig() {
        return jsonConfig;
    }

    public String getResourceType() {
        return HeliosCombinedResourceID.HttpEndpoint.DEFAULT;
    }

    public String getAllowAnonymous() {
        return allowAnonymous;
    }

    public String getMethodGet() {
        return methodGet;
    }

    public String getMethodHead() {
        return methodHead;
    }

    public String getMethodPost() {
        return methodPost;
    }

    public String getMethodPut() {
        return methodPut;
    }

    public String getMethodDelete() {
        return methodDelete;
    }

    public String getMethodOptions() {
        return methodOptions;
    }

    public String getMethodTrace() {
        return methodTrace;
    }

    public String getMethodPatch() {
        return methodPatch;
    }
}
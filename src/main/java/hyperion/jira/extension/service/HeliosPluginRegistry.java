package hyperion.jira.extension.service;

import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import hyperion.jira.extension.ao.Repository;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import hyperion.jira.extension.constant.HeliosResourceID;
import hyperion.jira.extension.context.script.SystemManagerContext;
import hyperion.jira.extension.context.template.TemplateContext;
import hyperion.jira.extension.service.providers.resourcetype.HTTPEndpointResourceType;
import hyperion.jira.interop.constants.HyperionResourceID;
import hyperion.jira.interop.constants.ResourceEditorID;
import hyperion.jira.interop.models.*;
import hyperion.jira.interop.services.PluginRegistry;
import hyperion.jira.interop.utils.IOUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;


/**
 * This is the main interface for providing data for Hyperion host plugin. Everything here, for distinguishability sake, is prefixed with [Helios],
 * but when you write your own extension plugins then prefixing is not necessary, as long as your resources don't collide with other extension plugins.
 */
@ExportAsService
@Component
public class HeliosPluginRegistry implements PluginRegistry {
    private final Repository repository;

    @Autowired
    public HeliosPluginRegistry(Repository repository) {
        this.repository = repository;
    }

    public String getName() {
        return "Helios Plugin";
    }

    public String getId() {
        return "Helios";
    }

    public String getScriptRootContextTypeScript() throws Exception {
        return IOUtil.getFileContent(this.getClass(), "helios/context/script/Root.ts");
    }

    /**
     * This method returns additional functions to be used in templates
     */
    public Object getTemplateContext() {
        return new TemplateContext();
    }

    /**
     * This method returns additional APIs to be used in scripts
     */
    public List<ScriptContext> getScriptContexts() throws Exception {
        List<ScriptContext> globalContexts = new ArrayList<>();
        globalContexts.add(new ScriptContext("SystemManager", new SystemManagerContext(), IOUtil.getFileContent(this.getClass(), "helios/context/script/SystemManager.ts")));
        return globalContexts;
    }

    /**
     * This method returns the list of available resource types.
     */
    public List<ResourceType> getResourceTypes() throws Exception {
        List<ResourceType> resourceTypes = new ArrayList<>();
        resourceTypes.add(HTTPEndpointResourceType.getResourceType());
        return resourceTypes;
    }

    /**
     * This method returns the list of templates that are not originating from this plugin.
     */
    public List<ResourceTemplate> getTemplates() throws Exception {
        List<ResourceTemplate> templates = new LinkedList<>();
        List<ResourceContent> assigneesOfOpenSubTasksContent = new ArrayList<>();
        assigneesOfOpenSubTasksContent.add(new ResourceContent(ResourceEditorID.SCRIPT, IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/Script.ts"), IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/Script.jss")));
        assigneesOfOpenSubTasksContent.add(new ResourceContent(ResourceEditorID.VIEW_TEMPLATE, IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/IssueView.vm")));
        assigneesOfOpenSubTasksContent.add(new ResourceContent(ResourceEditorID.SEARCH_COLUMN_TEMPLATE, IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/SearchColumn.vm")));
        assigneesOfOpenSubTasksContent.add(new ResourceContent(ResourceEditorID.SEARCH_XML_TEMPLATE, IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/SearchXML.vm")));
        assigneesOfOpenSubTasksContent.add(new ResourceContent(ResourceEditorID.CONFIG, IOUtil.getFileContent(this.getClass(), "helios/calculated-custom-fields/multi-user/assignees-of-open-sub-tasks/Config.json")));
        String parentResourceType = String.format("%s.%s.%s", HyperionResourceID.PLUGIN_ID, HyperionResourceID.CustomField.ID, HyperionResourceID.CustomField.SubType.CALCULATED_MULTI_USER);
        templates.add(new ResourceTemplate(HeliosResourceID.CustomField.Template.ASSIGNEES_OF_OPEN_SUB_TASKS, "[Helios] Assignees of Open Sub-Tasks", "Returns assignees of sub-tasks that are not in closed status. This example demonstrates how to write more advanced calculated custom field with a custom Velocity template and how to pass along additional parameters into Velocity Template.", ResourceTemplate.AccessLevel.EDITABLE_AND_RUNNABLE, parentResourceType, assigneesOfOpenSubTasksContent, true));
        return templates;
    }

    /**
     * This method returns the list of available libraries. Note that HttpClient has version range specified, which is optional, but as different JIRA versions have different HttpClient's included then version range can be specified.
     */
    public List<Library> getLibraries() throws Exception {
        List<Library> libraries = new ArrayList<>();
        libraries.add(new Library(HyperionResourceID.Library.Template.HTTP_CLIENT, "[Helios] Apache HttpClient", "4.4.1", IOUtil.getFileContent(this.getClass(), "helios/context/script/libraries/http-client/HttpClient4_4_1.ts"), new VersionRange(ResourceVersion.LOWER_UNBOUND, ResourceVersion.V7_3)));
        libraries.add(new Library(HyperionResourceID.Library.Template.HTTP_CLIENT, "[Helios] Apache HttpClient", "4.5.3", IOUtil.getFileContent(this.getClass(), "helios/context/script/libraries/http-client/HttpClient4_5_3.ts"), new VersionRange(ResourceVersion.V7_4, ResourceVersion.V7_7)));
        libraries.add(new Library(HyperionResourceID.Library.Template.HTTP_CLIENT, "[Helios] Apache HttpClient", "4.5.4", IOUtil.getFileContent(this.getClass(), "helios/context/script/libraries/http-client/HttpClient4_5_4.ts"), new VersionRange(ResourceVersion.V7_8, ResourceVersion.V7_8)));
        libraries.add(new Library(HyperionResourceID.Library.Template.HTTP_CLIENT, "[Helios] Apache HttpClient", "4.5.5", IOUtil.getFileContent(this.getClass(), "helios/context/script/libraries/http-client/HttpClient4_5_5.ts"), new VersionRange(ResourceVersion.V7_9, ResourceVersion.UPPER_UNBOUND)));
        return libraries;
    }

    /**
     * This method gets called when some resource gets unpublished, so warning could be sent back if those resources are still in use.
     */
    public List<UnpublishConflict> getUnpublishConflicts(String resourceID) {
        List<UnpublishConflict> conflicts = new ArrayList<>();
        String[] ids = resourceID.split(Pattern.quote("."));
        String pluginId = ids[0];
        String resourceTypeId = ids[1];

        if (pluginId.equals(HeliosResourceID.PLUGIN_ID)) {
            switch (resourceTypeId) {
                case HeliosResourceID.HttpEndpoint.ID:
                    checkHTTPEndpointConflicts(conflicts, resourceID);
                    break;
            }
        }
        return conflicts;
    }

    /**
     * This method gets called when resource is published
     */
    public void resourcePublished(String resourceID) {
    }

    /**
     * This method gets called when resource is unpublished
     */
    public void resourceUnpublished(String resourceID) {
    }

    private void checkHTTPEndpointConflicts(List<UnpublishConflict> conflicts, String resourceID) {
        UnpublishConflict conflict = new UnpublishConflict("[Helios] HTTP Endpoints");
        for (AOHTTPEndpoint endpoint : repository.getHttpEndpoints(resourceID)) {
            conflict.getConfiguredObjects().add("ID: " + endpoint.getID());
        }
        if (!conflict.getConfiguredObjects().isEmpty()) {
            conflicts.add(conflict);
        }
    }
}

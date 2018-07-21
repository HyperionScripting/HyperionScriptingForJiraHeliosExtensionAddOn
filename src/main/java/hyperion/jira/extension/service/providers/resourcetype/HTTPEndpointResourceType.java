package hyperion.jira.extension.service.providers.resourcetype;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import hyperion.jira.extension.constant.HeliosResourceID;
import hyperion.jira.extension.executors.HTTPEndpointExecutor;
import hyperion.jira.extension.instrumentation.params.HTTPEndpointParams;
import hyperion.jira.extension.test.live.params.HTTPEndpointLiveTestParams;
import hyperion.jira.interop.configs.DescribableConfig;
import hyperion.jira.interop.constants.HyperionResourceID;
import hyperion.jira.interop.constants.ResourceEditorID;
import hyperion.jira.interop.constants.ResourceEditorType;
import hyperion.jira.interop.exceptions.HandledException;
import hyperion.jira.interop.instrumentation.Execution;
import hyperion.jira.interop.managers.JSONManager;
import hyperion.jira.interop.managers.ScriptManager;
import hyperion.jira.interop.managers.TemplateManager;
import hyperion.jira.interop.models.*;
import hyperion.jira.interop.utils.IOUtil;

import java.io.IOException;
import java.util.*;

/**
 * This resource endpoint definition describes which editors are available, which templates it comes with and how to run Live Tests.
 */
public class HTTPEndpointResourceType {
    public static ResourceType getResourceType() throws IOException {
        List<ResourceSubType> resourceSubTypes = new LinkedList<>();
        resourceSubTypes.add(getDefaultSubType());
        return new ResourceType(HeliosResourceID.HttpEndpoint.ID, "[Helios] HTTP Endpoint", "[Helios] HTTP Endpoints", "HTTP Endpoints allow to respond to HTTP requests, this specific endpoint sends back HTML. This resource type is published by Helios extension add-on.", "http://hyperionscripting.com/doc/jira/scripting/latest/#http-endpoints", resourceSubTypes);
    }

    private static ResourceSubType getDefaultSubType() throws IOException {
        List<ResourceEditor> editors = new LinkedList<>();
        List<ResourceTemplate> templates = new LinkedList<>();
        List<ResourceTestType> testTypes = new LinkedList<>();
        String typeScriptDefinitions = IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/definitions.ts");
        String jsonSchema = IOUtil.getFileContent(HTTPEndpointResourceType .class, "helios/http-endpoint/schema.json");
        editors.add(new ResourceEditor(ResourceEditorID.SCRIPT, "Script", typeScriptDefinitions, ResourceEditorType.TYPESCRIPT));
        editors.add(new ResourceEditor(ResourceEditorID.TEMPLATE, "Template", ResourceEditorType.VELOCITY));
        editors.add(new ResourceEditor(ResourceEditorID.CONFIG, "Config", jsonSchema, ResourceEditorType.JSON, DescribableConfig.class));

        List<ResourceContent> emptyContent = new ArrayList<>();
        emptyContent.add(new ResourceContent(ResourceEditorID.SCRIPT, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/empty.content.ts")));
        emptyContent.add(new ResourceContent(ResourceEditorID.TEMPLATE, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/empty.content.vm")));
        emptyContent.add(new ResourceContent(ResourceEditorID.CONFIG, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/default.content.json")));
        templates.add(new ResourceTemplate("Blank", "Blank", "Blank template.", ResourceTemplate.AccessLevel.EDITABLE_ONLY, emptyContent));

        List<ResourceContent> systemInfoEndpointContent = new ArrayList<>();
        systemInfoEndpointContent.add(new ResourceContent(ResourceEditorID.SCRIPT, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/system-info/Script.ts"), IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/system-info/Script.jss"), Collections.singletonList(HyperionResourceID.SharedScript.Template.JQL_UTILS)));
        systemInfoEndpointContent.add(new ResourceContent(ResourceEditorID.TEMPLATE, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/system-info/Template.vm")));
        systemInfoEndpointContent.add(new ResourceContent(ResourceEditorID.CONFIG, IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/system-info/Config.json")));
        templates.add(new ResourceTemplate(HeliosResourceID.HttpEndpoint.Template.SYSTEM_INFO, "System Info", "HTTP Endpoint that demonstrates how to use custom APIs in scripts and custom functions in templates.", ResourceTemplate.AccessLevel.EDITABLE_AND_RUNNABLE, systemInfoEndpointContent, true));

        testTypes.add(new ResourceTestType("Execute", "Execute HTTP Endpoint", IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/test.content.json"), IOUtil.getFileContent(HTTPEndpointResourceType.class, "helios/http-endpoint/test.schema.json"), HTTPEndpointResourceType::runLiveTest));
        return new ResourceSubType(HeliosResourceID.HttpEndpoint.SubType.DEFAULT, "Default", null, editors, templates, testTypes);
    }

    private static LiveTestRunResult runLiveTest(ScriptManager scriptManager, TemplateManager templateManager, JSONManager jsonManager, Execution execution, Map<String, HyperionScript> scripts, String testParams, String rawAdminConfig, String resourceID) throws HandledException {
        Execution processTestParamsExecution = execution.newExecution("Process Test Params", "Serializes and processes test parameters specified for live testing in IDE's Test Params tab.", HTTPEndpointResourceType.class);
        HTTPEndpointLiveTestParams liveTestParams;
        String method = null;
        try {
            liveTestParams = jsonManager.fromJSON(testParams, HTTPEndpointLiveTestParams.class);
            method = liveTestParams.getMethod();
            processTestParamsExecution.success();
        } catch (Exception e) {
            processTestParamsExecution.error("Error while processing live test params - Resource ID: " + resourceID, e);
            processTestParamsExecution.stopExecution(e);
        }
        HTTPEndpointParams params = HTTPEndpointExecutor.getHTTPEndpointExecutionParams(0, null, method);
        execution.getDiagnosticsCollector().setParams(params);

        Execution processUserConfigExecution = execution.newExecution("Process Admin Config", "Serializes and processes admin configuration specified for live testing in IDE's Admin Config tab.", HTTPEndpointResourceType.class);
        Object adminConfig = null;
        try {
            adminConfig = jsonManager.fromJSONUsingJS(rawAdminConfig);
            processUserConfigExecution.success();
        } catch (Exception ex) {
            processUserConfigExecution.error("Error while processing admin config for HTTP Endpoint - Resource ID: " + resourceID, ex);
            processUserConfigExecution.stopExecution(ex);
        }
        String baseUrl = ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL);
        String html = HTTPEndpointExecutor.execute(null, null, HTTPEndpointExecutor.getJSMethodName(method), 0, scriptManager, templateManager, execution, resourceID, baseUrl, adminConfig, scripts);
        return new LiveTestRunResult(html, true);
    }
}

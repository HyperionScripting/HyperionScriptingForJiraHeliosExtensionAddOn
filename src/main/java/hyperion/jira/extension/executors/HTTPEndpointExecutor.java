package hyperion.jira.extension.executors;

import hyperion.jira.extension.instrumentation.params.HTTPEndpointParams;
import hyperion.jira.extension.servlet.models.HTTPEndpointExecutionContext;
import hyperion.jira.interop.constants.ResourceEditorID;
import hyperion.jira.interop.exceptions.HandledException;
import hyperion.jira.interop.instrumentation.Execution;
import hyperion.jira.interop.instrumentation.Runtime;
import hyperion.jira.interop.managers.ScriptManager;
import hyperion.jira.interop.managers.TemplateManager;
import hyperion.jira.interop.models.HyperionScript;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class HTTPEndpointExecutor {
    private static final Logger logger = LoggerFactory.getLogger(HTTPEndpointExecutor.class);

    public static String execute(HttpServletRequest request, HttpServletResponse response, String method, int httpEndpointId, ScriptManager scriptManager, TemplateManager templateManager, Execution execution, String resourceID, String baseUrl, Object adminConfig, Map<String, HyperionScript> scripts) throws HandledException {
        Execution processEndpointExecution = execution.newExecution("Process Endpoint", "Processes HTTP Endpoint.", HTTPEndpointExecutor.class);
        String renderedOutput = null;
        try {
            Execution executeScriptExecution = processEndpointExecution.newExecution("Execute Script (" + method + ")", "Executes '" + method + "' function.", HTTPEndpointExecutor.class);
            Object output = null;
            try {
                HTTPEndpointExecutionContext context = new HTTPEndpointExecutionContext(request, response, adminConfig);
                Runtime scriptRuntime = new Runtime(executeScriptExecution, scripts != null);
                if (scripts == null) {
                    output = scriptManager.executeScript(resourceID, ResourceEditorID.SCRIPT, method, scriptRuntime, context);
                } else {
                    output = scriptManager.executeScript(resourceID, ResourceEditorID.SCRIPT, scripts.get(ResourceEditorID.SCRIPT), method, scriptRuntime, context);
                }
                executeScriptExecution.success();
            } catch (InvocationTargetException ite) {
                Throwable rootCause = ExceptionUtils.getRootCause(ite);
                if (rootCause instanceof HandledException) {
                    throw new HandledException(rootCause);
                } else {
                    executeScriptExecution.error("Error while executing HTTP Endpoint function '" + method + "' - Resource ID: " + resourceID + " - HTTP Endpoint ID: " + httpEndpointId, ite);
                    executeScriptExecution.stopExecution(ite);
                }
            }
            Execution renderTemplateExecution = processEndpointExecution.newExecution("Execute Templates", "Renders template.", HTTPEndpointExecutor.class);
            try {
                Runtime templateRuntime = new Runtime(renderTemplateExecution, scripts != null);
                Map<String, Object> velocityParams = new HashMap<>();
                velocityParams.put("scriptParams", output);
                if (scripts == null) {
                    renderedOutput = templateManager.renderTemplate(resourceID, ResourceEditorID.TEMPLATE, templateRuntime, baseUrl, velocityParams);
                } else {
                    renderedOutput = templateManager.renderTemplate(resourceID, ResourceEditorID.TEMPLATE, scripts.get(ResourceEditorID.TEMPLATE), templateRuntime, baseUrl, velocityParams);
                }
                renderTemplateExecution.success();
                processEndpointExecution.success();
            } catch (Exception e) {
                renderTemplateExecution.error("Error while rendering HTTP Endpoint template - Resource ID: " + resourceID + " - HTTP Endpoint ID: " + httpEndpointId, e);
                processEndpointExecution.stopExecution(e);
            }
        } catch (Exception e) {
            processEndpointExecution.error("Error while processing HTTP Endpoint - Resource ID: " + resourceID + " - HTTP Endpoint ID: " + httpEndpointId, e);
            processEndpointExecution.stopExecution(e);
        }
        return renderedOutput;
    }

    public static HTTPEndpointParams getHTTPEndpointExecutionParams(int endpointId, String url, String method) {
        return new HTTPEndpointParams(endpointId, url, method);
    }

    public static void setHTTPEndpointExecutionResponseParams(HttpServletResponse response, HTTPEndpointParams params) {
        try {
            params.setStatus(response.getStatus());
            for (String header : response.getHeaderNames()) {
                params.addHeader(header, response.getHeader(header));
            }
        } catch (Exception e) {
            logger.error("[Hyperion] Error while setting HTTP Endpoint response params", e);
        }
    }

    public static String getJSMethodName(String method) {
        switch (method.toUpperCase()) {
            case "GET":
                return "doGet";
            case "HEAD":
                return "doHead";
            case "POST":
                return "doPost";
            case "PUT":
                return "doPut";
            case "DELETE":
                return "doDelete";
            case "OPTIONS":
                return "doOptions";
            case "TRACE":
                return "doTrace";
            case "PATCH":
                return "doPatch";
            default:
                throw new RuntimeException("Method not found: " + method);
        }
    }
}

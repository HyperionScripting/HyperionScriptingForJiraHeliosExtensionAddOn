package hyperion.jira.extension.servlet;

import com.atlassian.cache.CacheException;
import com.atlassian.cache.CacheManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import hyperion.jira.extension.ao.models.AOHTTPEndpoint;
import hyperion.jira.extension.exceptions.HTTPEndpointNotFoundException;
import hyperion.jira.extension.executors.HTTPEndpointExecutor;
import hyperion.jira.extension.instrumentation.params.HTTPEndpointParams;
import hyperion.jira.extension.manager.HeliosCacheManager;
import hyperion.jira.extension.service.ServiceManager;
import hyperion.jira.extension.servlet.models.HTTPEndpointCachedEntry;
import hyperion.jira.extension.utils.ClusterUtils;
import hyperion.jira.extension.utils.UserUtils;
import hyperion.jira.interop.constants.HyperionResourceID;
import hyperion.jira.interop.exceptions.HandledException;
import hyperion.jira.interop.instrumentation.DiagnosticsCollector;
import hyperion.jira.interop.instrumentation.Execution;
import hyperion.jira.interop.managers.InstrumentationManager;
import hyperion.jira.interop.managers.ScriptManager;
import hyperion.jira.interop.managers.TemplateManager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Scanned
public class HeliosServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HeliosServlet.class);
    private final ScriptManager scriptManager;
    private final TemplateManager templateManager;
    private final HeliosCacheManager cacheManager;
    private final InstrumentationManager instrumentationManager;
    private final JiraAuthenticationContext authenticationContext;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public HeliosServlet(@ComponentImport ScriptManager scriptManager, @ComponentImport TemplateManager templateManager, HeliosCacheManager cacheManager, @ComponentImport InstrumentationManager instrumentationManager, @ComponentImport JiraAuthenticationContext authenticationContext, @ComponentImport ApplicationProperties applicationProperties) {
        this.scriptManager = scriptManager;
        this.templateManager = templateManager;
        this.cacheManager = cacheManager;
        this.instrumentationManager = instrumentationManager;
        this.authenticationContext = authenticationContext;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "GET");
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "HEAD");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "POST");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "PUT");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "DELETE");
    }

    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "OPTIONS");
    }

    @Override
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "TRACE");
    }

    protected void doPatch(HttpServletRequest request, HttpServletResponse response) {
        processRequest(request, response, "PATCH");
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (request.getMethod().equalsIgnoreCase("PATCH")){
            doPatch(request, response);
        } else {
            super.service(request, response);
        }
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response, String method) {
        try {
            String path = request.getPathInfo();
            if (path == null) {
                send404(response);
                return;
            }
            String[] paths = path.split("/");
            if (paths.length == 0) {
                send404(response);
                return;
            }
            HTTPEndpointCachedEntry cachedEntry = cacheManager.getHttpEndpointsCache().get(paths[1]);
            AOHTTPEndpoint endpoint = cachedEntry.getHttpEndpoint();
            if (endpoint == null) {
                send404(response);
                return;
            }
            if (!endpoint.isAllowAnonymous() && authenticationContext.getLoggedInUser() == null) {
                send401(response);
                return;
            }
            if (!isMethodAllowed(endpoint, method, response)) {
                return;
            }
            try {
                logger.info("[Helios] Executing HTTP Endpoint - Resource ID: "+endpoint.getResourceID()+" - HTTP Endpoint ID: "+endpoint.getID()+" - URL: "+request.getPathInfo()+" - Method: "+method);
                HTTPEndpointParams params = HTTPEndpointExecutor.getHTTPEndpointExecutionParams(endpoint.getID(), request.getPathInfo(), method);
                DiagnosticsCollector diagnosticsCollector = new DiagnosticsCollector(endpoint.getResourceID(), params, "Execute HTTP Endpoint", HyperionResourceID.PLUGIN_ID, UserUtils::getUserName, UserUtils::getIP, UserUtils::getUserAgent, ClusterUtils::getNodeID, "HTTP Endpoint ID: "+endpoint.getID()+" ("+method+")");
                Execution mainExecution = diagnosticsCollector.newExecution("Process Extension Point", "Processes HTTP Endpoint extension point.", this.getClass());
                try {
                    String html = HTTPEndpointExecutor.execute(request, response, HTTPEndpointExecutor.getJSMethodName(method), endpoint.getID(), scriptManager, templateManager, mainExecution, endpoint.getResourceID(), applicationProperties.getString(APKeys.JIRA_BASEURL), cachedEntry.getJsonConfig(), null);
                    HTTPEndpointExecutor.setHTTPEndpointExecutionResponseParams(response, params);
                    response.setHeader("Content-Type", "text/html; charset=utf-8");
                    response.getWriter().write(html);
                    mainExecution.success();
                    diagnosticsCollector.success();
                } catch (HandledException he) {
                    mainExecution.fail();
                    diagnosticsCollector.fail();
                    send500(response);
                } finally {
                    instrumentationManager.storeDiagnostics(diagnosticsCollector);
                }
            } catch (Exception e) {
                logger.error("[Hyperion] Error while executing HTTP Endpoint - Resource ID: " + endpoint.getResourceID()+" - HTTP Endpoint ID: "+endpoint.getID(), e);
                send500(response);
            }
        } catch (CacheException e) {
            if (ExceptionUtils.getRootCause(e) instanceof HTTPEndpointNotFoundException) {
                send404(response);
            } else {
                logger.error("[Hyperion] Error while executing HTTP Endpoint - URL: "+request.getPathInfo(), e);
                send500(response);
            }
        } catch (Exception e) {
            logger.error("[Hyperion] Error while executing HTTP Endpoint - URL: "+request.getPathInfo(), e);
            send500(response);
        }
    }

    private boolean isMethodAllowed(AOHTTPEndpoint endpoint, String method, HttpServletResponse response) {
        switch (method) {
            case "GET":
                if (!endpoint.isMethodGet()) {
                    send405(response);
                    return false;
                }
                break;
            case "HEAD":
                if (!endpoint.isMethodHead()) {
                    send405(response);
                    return false;
                }
                break;
            case "POST":
                if (!endpoint.isMethodPost()) {
                    send405(response);
                    return false;
                }
                break;
            case "PUT":
                if (!endpoint.isMethodPut()) {
                    send405(response);
                    return false;
                }
                break;
            case "DELETE":
                if (!endpoint.isMethodDelete()) {
                    send405(response);
                    return false;
                }
                break;
            case "OPTIONS":
                if (!endpoint.isMethodOptions()) {
                    send405(response);
                    return false;
                }
                break;
            case "TRACE":
                if (!endpoint.isMethodTrace()) {
                    send405(response);
                    return false;
                }
                break;
            case "PATCH":
                if (!endpoint.isMethodPatch()) {
                    send405(response);
                    return false;
                }
                break;
        }
        return true;
    }

    private void send401(HttpServletResponse response) {
        response.setStatus(401);
        response.setHeader("WWW-Authenticate", "Basic realm=\"JIRA\"");
        response.setContentType("text/html");
        try {
            PrintWriter writer = response.getWriter();
            writer.append(getWrappedHTML("Unauthorized, please log in."));
        } catch (Exception e) {}
    }

    private void send404(HttpServletResponse response) {
        response.setStatus(404);
        response.setContentType("text/html");
        try {
            PrintWriter writer = response.getWriter();
            writer.append(getWrappedHTML("HTTP Endpoint not found."));
        } catch (Exception e) {}
    }

    private void send405(HttpServletResponse response) {
        response.setStatus(405);
        response.setContentType("text/html");
        try {
            PrintWriter writer = response.getWriter();
            writer.append(getWrappedHTML("Method not allowed."));
        } catch (Exception e) {}
    }

    private void send500(HttpServletResponse response) {
        response.setStatus(500);
        response.setContentType("text/html");
        try {
            PrintWriter writer = response.getWriter();
            writer.append(getWrappedHTML("Internal server error, please check the logs."));
        } catch (Exception e) { }
    }

    private String getWrappedHTML(String message) {
        return "<html lang=\"en\"><head><meta charset=\"utf-8\"><title>"+message+"</title></head><body><h1>"+message+"</h1></body></html>";
    }
}

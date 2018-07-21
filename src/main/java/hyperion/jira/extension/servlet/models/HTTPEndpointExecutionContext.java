package hyperion.jira.extension.servlet.models;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HTTPEndpointExecutionContext {
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Object adminConfig;

    public HTTPEndpointExecutionContext(HttpServletRequest request, HttpServletResponse response, Object adminConfig) {
        this.request = request;
        this.response = response;
        this.adminConfig = adminConfig;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public Object getAdminConfig() {
        return adminConfig;
    }

    public void setAdminConfig(Object adminConfig) {
        this.adminConfig = adminConfig;
    }
}

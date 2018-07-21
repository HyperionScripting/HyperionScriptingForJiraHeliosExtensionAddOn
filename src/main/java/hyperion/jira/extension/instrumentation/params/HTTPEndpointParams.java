package hyperion.jira.extension.instrumentation.params;

import java.util.HashMap;
import java.util.Map;

public class HTTPEndpointParams {
    private int endpointId;
    private String url;
    private String method;
    private int status;
    private Map<String, String> headers = new HashMap<>();

    public HTTPEndpointParams(int endpointId, String url, String method) {
        this.endpointId = endpointId;
        this.url = url;
        this.method = method;
    }

    public int getEndpointId() {
        return endpointId;
    }

    public String getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void addHeader(String header, String value) {
        headers.put(header, value);
    }
}

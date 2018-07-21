package hyperion.jira.extension.servlet.models;

import hyperion.jira.extension.ao.models.AOHTTPEndpoint;

public class HTTPEndpointCachedEntry {
    private AOHTTPEndpoint httpEndpoint;
    private Object jsonConfig;

    public HTTPEndpointCachedEntry(AOHTTPEndpoint httpEndpoint, Object jsonConfig) {
        this.httpEndpoint = httpEndpoint;
        this.jsonConfig = jsonConfig;
    }

    public AOHTTPEndpoint getHttpEndpoint() {
        return httpEndpoint;
    }

    public Object getJsonConfig() {
        return jsonConfig;
    }
}

package hyperion.jira.extension.exceptions;

public class HTTPEndpointNotFoundException extends RuntimeException {
    public HTTPEndpointNotFoundException(String s) {
        super(s);
    }
}

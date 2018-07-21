package hyperion.jira.extension.constant;

public class HeliosCombinedResourceID {
    public static class HttpEndpoint {
        public static final String DEFAULT = String.format("%s.%s.%s", HeliosResourceID.PLUGIN_ID, HeliosResourceID.HttpEndpoint.ID, HeliosResourceID.HttpEndpoint.SubType.DEFAULT);
    }
}

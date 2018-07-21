package hyperion.jira.extension.constant;

public class HeliosResourceID {
    public static final String PLUGIN_ID = "Helios";
    public static class HttpEndpoint {
        public static final String ID = "HttpEndpoint";
        public static class SubType {
            public static final String DEFAULT = "Default";
        }
        public static class Template {
            public static final String SYSTEM_INFO = "SystemInfo";
        }
    }

    public static class CustomField {
        public static final String ID = "CustomField";
        public static class Template {
            public static final String ASSIGNEES_OF_OPEN_SUB_TASKS = "HeliosAssigneesOfOpenSubTasks";
        }
    }
}

package hyperion.jira.extension.context.template;

public class FormatUtils {
    public static String getMBs(long bytes) {
        return (bytes / 1024 / 1024) + " MB";
    }
}

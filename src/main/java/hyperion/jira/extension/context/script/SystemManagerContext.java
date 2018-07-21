package hyperion.jira.extension.context.script;

public class SystemManagerContext {
    public long getMaxHeapSize() {
        return Runtime.getRuntime().totalMemory();
    }
}

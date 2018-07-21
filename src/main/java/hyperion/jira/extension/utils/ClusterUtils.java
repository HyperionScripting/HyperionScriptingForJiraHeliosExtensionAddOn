package hyperion.jira.extension.utils;

import com.atlassian.jira.cluster.ClusterInfo;
import com.atlassian.jira.component.ComponentAccessor;

public class ClusterUtils {
    public static String getNodeID() {
        return ComponentAccessor.getOSGiComponentInstanceOfType(ClusterInfo.class).getNodeId();
    }
}

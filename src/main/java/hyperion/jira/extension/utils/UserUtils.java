package hyperion.jira.extension.utils;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.user.ApplicationUser;
import webwork.action.ServletActionContext;

import javax.servlet.http.HttpServletRequest;

public class UserUtils {
    public static String getUserName() {
        ApplicationUser user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
        if (user != null) {
            return user.getDisplayName()+" ("+user.getUsername()+")";
        } else {
            return null;
        }
    }

    public static String getIP() {
        HttpServletRequest request = ServletActionContext.getRequest();
        return getIP(request);
    }

    public static String getIP(HttpServletRequest request) {
        if (request != null) {
            return request.getRemoteAddr();
        } else {
            return null;
        }
    }

    public static String getUserAgent() {
        HttpServletRequest request = ServletActionContext.getRequest();
        return getUserAgent(request);
    }

    public static String getUserAgent(HttpServletRequest request) {
        if (request != null) {
            return request.getHeader("User-Agent");
        } else {
            return null;
        }
    }
}

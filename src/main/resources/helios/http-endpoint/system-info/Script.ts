/**
 * This HTTP Endpoint demonstrates how to access custom APIs in scripts and how to access custom functions in templates.
 */

const APKeys = Java.type("com.atlassian.jira.config.properties.APKeys");

const doGet: HttpEndpointFunction = (context, runtime) => {
    return {
        issuesAssignedToMe: getIssuesFromJQL("assignee = currentUser()").size(),
        baseUrl: ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL),
        maxHeapSize: Helios.SystemManager.getMaxHeapSize() //This demonstrates how to access custom APIs
    }
}

const getSampleAdminConfig: GetSampleAdminConfigFunction = () => {
    return null;
}
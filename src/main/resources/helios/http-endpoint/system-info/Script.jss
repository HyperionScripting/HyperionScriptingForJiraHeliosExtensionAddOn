var Hyperion;
var ComponentAccessor = Java.type("com.atlassian.jira.component.ComponentAccessor");
var Helios;
var SearchService = Java.type("com.atlassian.jira.bc.issue.search.SearchService");
var PageFilter = Java.type("com.atlassian.jira.web.bean.PagerFilter");
var StringUtils = Java.type("java.lang.String");
function getIssuesFromJQL(jql, user) {
    if (!user) {
        user = ComponentAccessor.getJiraAuthenticationContext().getLoggedInUser();
    }
    var searchService = ComponentAccessor.getComponentOfType(SearchService.class);
    var parseResult = searchService.parseQuery(user, jql);
    if (parseResult.isValid()) {
        return searchService.search(user, parseResult.getQuery(), PageFilter.getUnlimitedFilter()).getIssues();
    }
    else {
        throw StringUtils.join(", ", parseResult.getErrors().getErrorMessages());
    }
}
var APKeys = Java.type("com.atlassian.jira.config.properties.APKeys");
var doGet = function (context, runtime) {
    return {
        issuesAssignedToMe: getIssuesFromJQL("assignee = currentUser()").size(),
        baseUrl: ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL),
        maxHeapSize: Helios.SystemManager.getMaxHeapSize()
    };
};
var getSampleAdminConfig = function () {
    return null;
};

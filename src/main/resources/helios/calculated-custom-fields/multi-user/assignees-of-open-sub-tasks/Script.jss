var Hyperion;
var ComponentAccessor = Java.type("com.atlassian.jira.component.ComponentAccessor");
var ArrayList = Java.type("java.util.ArrayList");
var HashMap = Java.type("java.util.HashMap");
var Collectors = Java.type("java.util.stream.Collectors");
var Collections = Java.type("java.util.Collections");
var getValueFromIssue = function (context, runtime) {
    var assignees = new ArrayList();
    context.issue.getSubTaskObjects().forEach(function (subTask) {
        var subTaskExecution = runtime.execution.newExecution("Process Sub-Task: " + subTask.getKey());
        try {
            if (context.adminConfig.closedStatuses.indexOf(subTask.getStatus().getName()) == -1) {
                var assignee_1 = subTask.getAssignee();
                if (assignee_1 && !assignees.stream().anyMatch(function (a) { return a.getId() == assignee_1.getId(); })) {
                    assignees.add(assignee_1);
                    subTaskExecution.info("Add assignee to list: " + assignee_1.getDisplayName());
                }
            }
            subTaskExecution.success();
        }
        catch (e) {
            subTaskExecution.error("Error while processing sub-task: " + subTask.getKey(), e);
            subTaskExecution.fail();
        }
    });
    runtime.execution.info("Assignees found: " + assignees.size());
    return assignees;
};
var addAdditionalVelocityParameters = function (context, runtime) {
    var assigneesMap = new HashMap();
    var subTasks = context.issue.getSubTaskObjects();
    var closedStatuses = context.adminConfig.closedStatuses;
    context.value.forEach(function (assignee) {
        var assigneeExecution = runtime.execution.newExecution("Process Assignee: " + assignee.getName());
        try {
            var issuesAssigned = subTasks.stream().filter(function (st) { return st.getAssignee() && st.getAssignee().getId() == assignee.getId() && closedStatuses.indexOf(st.getStatus().getName()) == -1; }).count();
            assigneesMap.put(assignee, issuesAssigned);
            assigneeExecution.info("Issues assigned: " + issuesAssigned);
            assigneeExecution.success();
        }
        catch (e) {
            assigneeExecution.error("Error while processing assignee: " + assignee.getDisplayName(), e);
            assigneeExecution.fail();
        }
    });
    var sortedAssignees = assigneesMap.entrySet().stream().sorted(function (a, b) { return a.getValue().compareTo(b.getValue()); }).collect(Collectors.toList());
    Collections.reverse(sortedAssignees);
    context.velocityParams.put("assignees", sortedAssignees);
};
var getSampleAdminConfig = function () {
    var sampleAdminConfig = {
        closedStatuses: ["Done"]
    };
    return JSON.stringify(sampleAdminConfig);
};
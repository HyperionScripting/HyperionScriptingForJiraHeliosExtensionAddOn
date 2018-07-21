/**
 * This calculated custom field retrieves the list of assignees of sub-tasks that are not in closed status (configurable) and will display number of assigned
 * sub-tasks after the user name. It is imporant to note that the logic is spit between 2 functions: 'getValueFromIssue' and 'addAdditionalVelocityParameters'.
 * Importance of that is the function 'getValueFromIssue' can only return valid data type which in our case is a Java collection of JIRA users (Java.Collection<Jira.ApplicationUser>),
 * and because of that there is no way to piggyback the number of assigned issues. JIRA defines strict rules for data types that custom fields can return, so it would work with
 * existing indexers, searchers, and other functionality that JIRA knows how to handle.
 * To pass along the number of assigned issues for each assigne we will do it in 'addAdditionalVelocityParamters' function that gets executed after 'getValueFromIssue' prior to
 * rendering the template, and we will render the template using a list of ordered assignees and number of issues for each assignee that we will calculate in 'addAdditionalVelocityParamters' function.
 * This example demonstrates how to write your custom template and how to pass along extra data, that is needed to renderd the template, that otherwise cannot be passed while
 * calculating the initial value of the custom field. To write your own templates and to have 'addAdditionalVelocityParameters' function executed you have to ovewrite
 * corresponding fields in Config tab.
 * Note that since the number of issues for each assignee is not initially known (output of 'getValueFromIssue' function) then number of issues will not be displayed in CSV export and
 * REST calls due to how JIRA handles those operations internally by calling 'getValueFromIssue' and then using the returned value directly without rendering the template
 * and therefor not calling 'addAdditionalVelocityParamters' that is responsible for calculating issue count.
 */

//Retrieve Java classes for later use
const ArrayList = Java.type("java.util.ArrayList");
const HashMap = Java.type("java.util.HashMap");
const Collectors = Java.type("java.util.stream.Collectors");
const Collections = Java.type("java.util.Collections");

//In this function, which gets executed first, we will retrieve the distinct list of assignees of opened sub-tasks
const getValueFromIssue: GetValueFromIssueFunction<AdminConfig> = (context, runtime) => {
    //Initialize a list for holding assignees
    const assignees = new ArrayList<Jira.ApplicationUser>();
    //Iterate over all the sub-tasks
    context.issue.getSubTaskObjects().forEach(subTask => {
        //Create new sub-execution for processing sub-task
        const subTaskExecution = runtime.execution.newExecution("Process Sub-Task: " + subTask.getKey());
        try {
            //Continue only when sub-tasks is not in one of the closed statuses
            if (context.adminConfig.closedStatuses.indexOf(subTask.getStatus().getName()) == -1) {
                //Re-reference assignee of the sub-task
                const assignee = subTask.getAssignee();
                //Check if the sub-task has assignee defined and if that assignee is not already in assignees list, as we want to retain only the distinct list of assignees
                if (assignee && !assignees.stream().anyMatch(a => a.getId() == assignee.getId())) {
                    //Add assignee to assignees list
                    assignees.add(assignee);
                    //Log the assignee that was added to list
                    subTaskExecution.info("Add assignee to list: " + assignee.getDisplayName());
                }
            }
            //Mark the sub-execution as success
            subTaskExecution.success();
        } catch (e) {
            //In case error log it
            subTaskExecution.error("Error while processing sub-task: " + subTask.getKey(), e);
            //Mark the sub-execution as failure
            subTaskExecution.fail();
        }
    });
    //Log how many assignees were found
    runtime.execution.info("Assignees found: " + assignees.size());
    //Return collected assignees
    return assignees;
}

//In this function which gets executed prior to rendering the template we will count how many issues each assignee has and pass it along to Velocity template
const addAdditionalVelocityParameters: AddAdditionalVelocityParametersFunction<AdminConfig> = (context, runtime) => {
    //Initialize a map for holding assignees as keys and number of assigned issues as values
    const assigneesMap = new HashMap<Jira.ApplicationUser, int>();
    //Get sub-tasks
    const subTasks = context.issue.getSubTaskObjects();
    //Get configured closed statuses
    const closedStatuses = context.adminConfig.closedStatuses;
    //Iterate over assignees that were retrieved from the initial 'getValueFromIssue' function, note the 'context.value' property that allows to retrieve the initial value
    context.value.forEach(assignee => {
        //Create new sub-execution for processing assignee
        const assigneeExecution = runtime.execution.newExecution("Process Assignee: "+assignee.getName());
        try {
            //Count all the sub-tasks where given assignee has been assigned to and only sub-tasks which are not closed
            const issuesAssigned = subTasks.stream().filter(st => st.getAssignee() && st.getAssignee().getId() == assignee.getId() && closedStatuses.indexOf(st.getStatus().getName()) == -1).count();
            //Add assignee and number of assigned issues to map
            assigneesMap.put(assignee, issuesAssigned);
            //Log how many sub-tasks were found for given assignee
            assigneeExecution.info("Issues assigned: "+issuesAssigned);
            //Mark the sub-execution as success
            assigneeExecution.success();
        } catch (e) {
            //In case of error log it
            assigneeExecution.error("Error while processing assignee: " + assignee.getDisplayName(), e);
            //Mark the sub-execution as failure
            assigneeExecution.fail();
        }
    });
    //Sort the map by assigned issues count and convert the map to list
    const sortedAssignees = assigneesMap.entrySet().stream().sorted((a, b) => a.getValue().compareTo(b.getValue())).collect(Collectors.toList());
    //Reverse the sorting order because by default the list is sorted ascendingly, we need it to be in descending order to show the people of highest number of assigned sub-tasks first
    Collections.reverse(sortedAssignees);
    //Add sorted assignees list to velocity parameters map that we will retrieve while rendering the template
    context.velocityParams.put("assignees", sortedAssignees);
}

//Returns sample admin config
const getSampleAdminConfig: GetSampleAdminConfigFunction = () => {
    const sampleAdminConfig: AdminConfig = {
        closedStatuses: ["Done"]
    }
    return JSON.stringify(sampleAdminConfig);
}

interface AdminConfig {
    closedStatuses: string[];
}
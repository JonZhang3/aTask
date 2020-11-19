package com.atask;

import com.atask.util.Utils;

import java.util.Collection;

public final class TaskStatService {

    protected static final TaskStatService INSTANCE = new TaskStatService();

    private TaskEngine taskEngine;

    private TaskStatService() {
    }

    public static void setTaskEngine(TaskEngine taskEngine) {
        INSTANCE.taskEngine = taskEngine;
    }

    protected String getBasicInfo() {
        Json.JsonObject object = Json.createObject();
        object.put("version", Utils.VERSION);
        object.put("java_version", Utils.getJavaVersion());
        object.put("os_info", Utils.getOsInfo());
        return object.end().toString();
    }

    protected String getTaskInfo() {
        Json.JsonObject object = Json.createObject();
        object.put("count_task", taskEngine.getTotalNumberOfTask());
        object.put("count_running_task", taskEngine.getRunningNumberofTask());
        object.put("count_completed_task", taskEngine.getCompletedNumberOfTask());
        Collection<TaskGroup> taskGroups = taskEngine.getRunningTaskGroups();
        object.put("count_running_taskgroup", taskGroups.size());
        Json.JsonArray groups = Json.createArray();
        for (TaskGroup group : taskGroups) {
            Json.JsonObject groupObject = Json.createObject()
                .put("name", group.getName())
                .put("id", group.getId())
                .put("count_running_task", group.getRunningTasks().size())
                .end();
            groups.add(groupObject);
        }
        object.put("groups", groups.end());
        return object.end().toString();
    }

}

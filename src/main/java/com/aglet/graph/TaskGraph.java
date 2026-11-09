package com.aglet.graph;

import com.aglet.domain.Task;
import com.aglet.domain.TaskState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TaskGraph {

    private final Map<UUID, Task> tasks;
    private final Map<UUID, Set<UUID>> prerequisites;
    private final Map<UUID, Set<UUID>> dependents;

    public TaskGraph(List<Task> taskList) {
        this.tasks = new HashMap<>();
        this.prerequisites = new HashMap<>();
        this.dependents = new HashMap<>();

        buildGraph(taskList);
    }

    private void buildGraph(List<Task> taskList) {
        for (Task task : taskList) {
            UUID taskId = task.getTaskId();

            tasks.put(taskId, task);
            prerequisites.put(
                    taskId,
                    new HashSet<>(task.getDependencies()));
            dependents.putIfAbsent(taskId, new HashSet<>());
        }

        for (Task task : taskList) {
            UUID taskId = task.getTaskId();

            for (UUID dependencyId : task.getDependencies()) {
                dependents
                        .computeIfAbsent(dependencyId, id -> new HashSet<>())
                        .add(taskId);
            }
        }
    }

    public Set<UUID> getInitialTasks() {
        Set<UUID> initialTasks = new HashSet<>();

        for (Map.Entry<UUID, Set<UUID>> entry : prerequisites.entrySet()) {
            if (entry.getValue().isEmpty()) {
                initialTasks.add(entry.getKey());
            }
        }

        return Collections.unmodifiableSet(initialTasks);
    }

    public Set<UUID> getDependents(UUID taskId) {
        return Collections.unmodifiableSet(
                dependents.getOrDefault(taskId, Set.of()));
    }

    public boolean arePrerequisitesCompleted(UUID taskId) {
        Set<UUID> prerequisiteIds = prerequisites.get(taskId);

        if (prerequisiteIds == null) {
            throw new IllegalArgumentException("Unknown task: " + taskId);
        }

        for (UUID prerequisiteId : prerequisiteIds) {
            Task prerequisite = tasks.get(prerequisiteId);

            if (prerequisite == null
                    || prerequisite.getState() != TaskState.COMPLETED) {
                return false;
            }
        }

        return true;
    }
}
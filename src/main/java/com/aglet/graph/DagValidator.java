package com.aglet.graph;

import com.aglet.domain.Task;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public class DagValidator {

    public void validate(List<Task> tasks) {
        validateDuplicateIds(tasks);
        validateDependenciesExist(tasks);
        validateNoCycles(tasks);
    }

    public void validateDuplicateIds(List<Task> tasks) {
        Set<UUID> taskIds = new HashSet<>();

        for (Task task : tasks) {
            if (!taskIds.add(task.getTaskId())) {
                throw new IllegalArgumentException(
                        "Duplicate task ID: " + task.getTaskId());
            }
        }
    }

    public void validateDependenciesExist(List<Task> tasks) {
        Set<UUID> taskIds = new HashSet<>();

        for (Task task : tasks) {
            taskIds.add(task.getTaskId());
        }

        for (Task task : tasks) {
            for (UUID dependencyId : task.getDependencies()) {
                if (!taskIds.contains(dependencyId)) {
                    throw new IllegalArgumentException(
                            "Missing dependency: " + dependencyId);
                }
            }
        }
    }

    public void validateNoCycles(List<Task> tasks) {
        Map<UUID, Integer> inDegree = new HashMap<>();
        Map<UUID, Set<UUID>> dependents = new HashMap<>();

        for (Task task : tasks) {
            UUID taskId = task.getTaskId();
            inDegree.put(taskId, task.getDependencies().size());
            dependents.put(taskId, new HashSet<>());
        }

        for (Task task : tasks) {
            UUID taskId = task.getTaskId();

            for (UUID dependencyId : task.getDependencies()) {
                dependents.get(dependencyId).add(taskId);
            }
        }

        Queue<UUID> readyTasks = new ArrayDeque<>();

        for (Map.Entry<UUID, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                readyTasks.add(entry.getKey());
            }
        }

        int processedTaskCount = 0;

        while (!readyTasks.isEmpty()) {
            UUID completedTaskId = readyTasks.remove();
            processedTaskCount++;

            for (UUID dependentId : dependents.get(completedTaskId)) {
                int remainingDependencies = inDegree.get(dependentId) - 1;
                inDegree.put(dependentId, remainingDependencies);

                if (remainingDependencies == 0) {
                    readyTasks.add(dependentId);
                }
            }
        }

        if (processedTaskCount != tasks.size()) {
            throw new IllegalArgumentException("Cycle detected in task graph");
        }
    }
}

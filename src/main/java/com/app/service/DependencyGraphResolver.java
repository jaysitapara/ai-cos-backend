package com.app.service;

import com.app.model.planner.TaskItem;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DependencyGraphResolver {

    /** Detects circular dependencies using DFS graph traversal */
    public boolean hasCircularDependency(List<TaskItem> tasks) {
        Map<String, List<String>> adjList = new HashMap<>();
        for (TaskItem task : tasks) {
            adjList.put(task.getTaskId(), task.getDependencies() != null ? task.getDependencies() : List.of());
        }

        Set<String> visited = new HashSet<>();
        Set<String> recStack = new HashSet<>();

        for (TaskItem task : tasks) {
            if (isCyclic(task.getTaskId(), adjList, visited, recStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCyclic(String node, Map<String, List<String>> adjList, Set<String> visited, Set<String> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        List<String> neighbors = adjList.getOrDefault(node, List.of());
        for (String neighbor : neighbors) {
            if (isCyclic(neighbor, adjList, visited, recStack)) {
                return true;
            }
        }

        recStack.remove(node);
        return false;
    }
}

/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.execution.taskgraph;

import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.Specs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultTaskExecutor implements TaskExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTaskExecutor.class);

    public void process(TaskExecutionPlan taskExecutionPlan, TaskExecutionListener taskListener) {
        Spec<TaskInfo> anyTask = Specs.satisfyAll();
        TaskInfo taskInfo = taskExecutionPlan.getTaskToExecute(anyTask);
        while (taskInfo != null) {
            executeTask(taskInfo, taskExecutionPlan, taskListener);
            taskInfo = taskExecutionPlan.getTaskToExecute(anyTask);
        }
        taskExecutionPlan.awaitCompletion();
    }

    protected void executeTask(TaskInfo taskInfo, TaskExecutionPlan taskExecutionPlan, TaskExecutionListener taskListener) {
        TaskInternal task = taskInfo.getTask();
        for (TaskInfo dependency : taskInfo.getDependencies()) {
            if (!dependency.isComplete()) {
                // Cannot execute this task, as some dependencies have not been executed
                String message = String.format("Cannot execute %s, as dependency %s has not been executed", task.getPath(), dependency.getTask().getPath());
                // TODO:DAZ This should not be warning
                LOGGER.warn(message);
                return;
            }
        }

        taskListener.beforeExecute(task);
        try {
            task.executeWithoutThrowingTaskFailure();
        } finally {
            taskListener.afterExecute(task, task.getState());
        }

        if (task.getState().getFailure() != null) {
            // TODO Not sure if we play well with --continue
            taskExecutionPlan.taskFailed(taskInfo);
        } else {
            taskExecutionPlan.taskComplete(taskInfo);
        }
    }
}

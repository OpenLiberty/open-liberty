/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.product.utility;

import java.util.HashMap;
import java.util.Map;

public class CommandTaskRegistry {

    private final Map<String, Class<? extends CommandTask>> nameCommandTaskClassMap = new HashMap<String, Class<? extends CommandTask>>();

    private final Map<String, CommandTask> nameCommandTaskInstanceMap = new HashMap<String, CommandTask>();

    public void registerCommandTask(String taskName, CommandTask commandTask) {
        taskName = taskName.toLowerCase();
        if (nameCommandTaskClassMap.containsKey(taskName)) {
            return;
        }
        nameCommandTaskClassMap.put(taskName, commandTask.getClass());
        nameCommandTaskInstanceMap.put(taskName, commandTask);

    }

    public void registerCommandTask(String taskName, Class<? extends CommandTask> taskClass) {
        taskName = taskName.toLowerCase();
        if (nameCommandTaskClassMap.containsKey(taskName)) {
            return;
        }
        nameCommandTaskClassMap.put(taskName, taskClass);
    }

    public void unRegisterCommandTask(String taskName) {
        taskName = taskName.toLowerCase();
        nameCommandTaskClassMap.remove(taskName);
        nameCommandTaskInstanceMap.remove(taskName);
    }

    public CommandTask getCommandTask(String taskName) {
        taskName = taskName.toLowerCase();
        CommandTask commandTask = nameCommandTaskInstanceMap.get(taskName);
        if (commandTask != null) {
            return commandTask;
        }
        try {
            Class<? extends CommandTask> commandTaskClass = nameCommandTaskClassMap.get(taskName);
            if (commandTaskClass == null) {
                return null;
            }
            commandTask = commandTaskClass.newInstance();
            nameCommandTaskInstanceMap.put(taskName, commandTask);
            return commandTask;
        } catch (InstantiationException e) {
            System.err.print(CommandUtils.getMessage("ERROR_UNABLE_INITIALIZE_TASK_INSTANCE", taskName, e.getMessage()));
            return null;
        } catch (IllegalAccessException e) {
            System.err.print(CommandUtils.getMessage("ERROR_UNABLE_INITIALIZE_TASK_INSTANCE", taskName, e.getMessage()));
            return null;
        }
    }

    public CommandTask[] getCommandTasks() {
        if (nameCommandTaskInstanceMap.size() == nameCommandTaskClassMap.size()) {
            return nameCommandTaskInstanceMap.values().toArray(new CommandTask[nameCommandTaskInstanceMap.size()]);
        }

        for (Map.Entry<String, Class<? extends CommandTask>> entry : nameCommandTaskClassMap.entrySet()) {
            String taskName = entry.getKey();
            if (nameCommandTaskInstanceMap.containsKey(taskName)) {
                continue;
            }
            Class<? extends CommandTask> commandTaskClass = entry.getValue();
            try {
                nameCommandTaskInstanceMap.put(taskName, commandTaskClass.newInstance());
            } catch (InstantiationException e) {
                System.err.print(CommandUtils.getMessage("ERROR_UNABLE_INITIALIZE_TASK_INSTANCE", taskName, e.getMessage()));
            } catch (IllegalAccessException e) {
                System.err.print(CommandUtils.getMessage("ERROR_UNABLE_INITIALIZE_TASK_INSTANCE", taskName, e.getMessage()));
            }
        }
        return nameCommandTaskInstanceMap.values().toArray(new CommandTask[nameCommandTaskInstanceMap.size()]);
    }

    public void clear() {
        nameCommandTaskClassMap.clear();
        nameCommandTaskInstanceMap.clear();
    }
}

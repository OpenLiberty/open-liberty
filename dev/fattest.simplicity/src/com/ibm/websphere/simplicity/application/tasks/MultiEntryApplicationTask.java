/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.application.tasks;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

/**
 * MEATs work differently than ATs. The superclass's taskData property
 * isn't used. Instead the list of entries makes up the task data, and
 * when the data is requested we build it from scratch each time.
 */
public abstract class MultiEntryApplicationTask extends ApplicationTask {

    private static final Class c = MultiEntryApplicationTask.class;

    protected ArrayList<TaskEntry> entries = new ArrayList<TaskEntry>();

    public MultiEntryApplicationTask() {
        super();
    }

    public MultiEntryApplicationTask(String taskName, String[][] taskData) {
        super(taskName, taskData);
    }

    public MultiEntryApplicationTask(String taskName, String[] columns) {
        super(taskName, columns);
        generateEntries();
    }

    public abstract Object get(int i);

    public void deleteAll() {
        modified = true;
        entries.clear();
    }

    public String[][] getTaskData() {
        if (entries.size() == 0)
            return null;

        String[][] ret = new String[entries.size() + 1][coltbl.size()];
        // Populate columns
        for (Map.Entry<String, Integer> entry : coltbl.entrySet()) {
            ret[0][entry.getValue()] = entry.getKey();
        }
        // Populate data
        for (int i = 0; i < entries.size(); i++) {
            String[] data = entries.get(i).getTaskData();
            for (int p = 0; p < coltbl.size(); p++)
                ret[i + 1][p] = data[p];
        }
        return ret;
    }

    public void delete(TaskEntry entry) {
        modified = true;
        entries.remove(entry);
    }

    public int size() {
        return (entries != null ? entries.size() : 0);
    }

    protected TaskEntry getEntry(String column, String value) {
        for (TaskEntry entry : entries) {
            if (entry.getString(column).equalsIgnoreCase(value))
                return entry;
        }
        return null;
    }

    protected List<? extends TaskEntry> getEntries(String column, String value) {
        List<TaskEntry> ret = new ArrayList<TaskEntry>();
        for (TaskEntry entry : entries) {
            if (entry.getString(column).equalsIgnoreCase(value))
                ret.add(entry);
        }
        return ret;
    }

    protected TaskEntry getEntry(String[] columns, String[] values) {
        for (TaskEntry entry : entries) {
            boolean match = true;
            for (int i = 0; i < columns.length && match; i++) {
                if (!entry.getString(columns[i]).equalsIgnoreCase(values[i]))
                    match = false;
            }
            if (match)
                return entry;
        }
        return null;
    }

    protected List<? extends TaskEntry> getEntries(String[] columns, String[] values) {
        List<TaskEntry> ret = new ArrayList<TaskEntry>();
        for (TaskEntry entry : entries) {
            boolean match = true;
            for (int i = 0; i < columns.length && match; i++) {
                if (!entry.getString(columns[i]).equalsIgnoreCase(values[i]))
                    match = false;
            }
            if (match)
                ret.add(entry);
        }
        return ret;
    }

    // This is probably unnecessary -- I think it's called at the wrong time to do anything
    private void generateEntries() {
        Log.entering(c, "generateEntries");
        this.entries = new ArrayList<TaskEntry>();
        try {
            for (int i = 1; i < taskData.length; i++) {
                String[] data = new String[taskData[i].length];
                for (int o = 0; o < taskData[i].length; o++)
                    data[o] = taskData[i][o];
                String className = this.getClass().getCanonicalName().replace("Task", "Entry");
                Log.debug(c, "class: " + className);
                Class clazz = Class.forName(className);

                Constructor constructor = clazz.getConstructor(String[].class, MultiEntryApplicationTask.class);
                TaskEntry entry = (TaskEntry) constructor.newInstance(data, this);

                this.entries.add(entry);
            }
            this.taskData = null;
        } catch (Exception e) {
            Log.error(c, "generateEntries", e);
        }
        Log.exiting(c, "generateEntries");
    }

}

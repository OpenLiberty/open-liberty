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

import java.util.Hashtable;

import com.ibm.websphere.simplicity.application.AppConstants;
import com.ibm.websphere.simplicity.exception.IncompatibleVersionException;

public abstract class ApplicationTask {

    protected boolean modified = false;

    public static class ColumnMetadata {

        public boolean isMutable = false;
        public boolean isRequired = false;
        public boolean isHidden = false;

        protected ColumnMetadata(boolean isMutable, boolean isRequired, boolean isHidden) {
            this.isMutable = isMutable;
            this.isRequired = isRequired;
            this.isHidden = isHidden;
        }

    }

    protected String taskName;
    protected String[][] taskData;
    protected Hashtable<String, Integer> coltbl = new Hashtable<String, Integer>();
    protected Hashtable<String, ColumnMetadata> colmeta = new Hashtable<String, ColumnMetadata>();

    public ApplicationTask() {
        taskData = new String[1][0];
    }

    public ApplicationTask(String taskName, String[][] taskData) {
        this.taskName = taskName;
        this.taskData = taskData;
        init();
    }

    public ApplicationTask(String taskName, String[] columns) {
        this.taskName = taskName;
        this.taskData = new String[1][columns.length];
        for (int i = 0; i < columns.length; i++)
            this.taskData[0][i] = columns[i];
        init();
    }

    public void setColumnMetadata(String name, boolean isMutable, boolean isRequired, boolean isHidden) {
        colmeta.put(name, new ColumnMetadata(isMutable, isRequired, isHidden));
    }

    public ColumnMetadata getColumnMetadata(String name) {
        return colmeta.get(name);
    }

    public String getTaskName() {
        return taskName;
    }

    public String[][] getTaskData() {
        return taskData;
    }

    public String toString() {
        StringBuilder s = new StringBuilder(getTaskName());
        String[][] data = this.getTaskData();
        if (data != null) {
            s.append("\n\t\t" + data.length + " entry(s)");
            for (int i = 0; i < data.length; i++) {
                s.append("\n\t\t");
                for (int o = 0; o < data[i].length; o++)
                    s.append(data[i][o] + " | ");
            }
            s.append("\n");
        } else
            s.append(": null\n");
        return s.toString();
    }

    // Earlier versions do not have certain columns
    protected void hasAtLeast(int columns) throws IncompatibleVersionException {
        if (taskData.length > 0 && taskData[0].length < columns)
            throw new IncompatibleVersionException("");
    }

    protected void setBoolean(String columnName, int row, boolean value) {
        setItem(columnName, row, value ? AppConstants.YES_KEY : AppConstants.NO_KEY);
    }

    protected void setInteger(String columnName, int row, int value) {
        setItem(columnName, row, Integer.toString(value));
    }

    protected void setItem(String columnName, int row, String value) {
        if (!coltbl.containsKey(columnName)) {
            addColumn(columnName);
        }
        Integer col = coltbl.get(columnName);
        if (col != null) {
            if (row >= taskData.length) {
                resizeTo(row + 1);
            }
            taskData[row][col] = value;
        }
    }

    protected boolean getBoolean(String columnName, int row) {
        return getBoolean(columnName, row, false);
    }

    protected boolean getBoolean(String columnName, int row, Boolean deflt) {
        String s = getItem(columnName, row);
        boolean b = false;
        if (s != null) {
            b = (s.equalsIgnoreCase(AppConstants.YES_KEY) || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"));
        }
        return b || deflt;
    }

    protected String getString(String columnName, int row) {
        return getString(columnName, row, null);
    }

    protected String getString(String columnName, int row, String deflt) {
        String s = getItem(columnName, row);
        if (s == null)
            s = deflt;
        return s;
    }

    protected Integer getInteger(String columnName, int row) {
        return getInteger(columnName, row, null);
    }

    protected Integer getInteger(String columnName, int row, Integer deflt) {
        String s = getItem(columnName, row);
        Integer ret = toInteger(s);
        if (ret == null)
            ret = deflt;
        return ret;
    }

    protected Integer toInteger(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    protected String getItem(String columnName, int row) {
        String ret = null;
        Integer col = coltbl.get(columnName);
        if (col != null && taskData.length > row)
            ret = taskData[row][col];
        return ret;
    }

    protected void init() {
        for (int i = 0; i < taskData[0].length; i++)
            coltbl.put(taskData[0][i], i);
    }

    /**
     * Resizes the first dimension of the taskData array.
     * The |rows| parameter *includes* the column headings,
     * so the entire dimension will be size |rows|.
     * 
     * @param rows
     */
    private void resizeTo(int rows) {
        String[][] newData = new String[rows][coltbl.size()];
        for (int r = 0; r < taskData.length; r++) {
            for (int c = 0; c < coltbl.size(); c++)
                newData[r][c] = taskData[r][c];
        }
        taskData = newData;
    }

    private void addColumn(String name) {
        String[][] newData = new String[taskData.length][coltbl.size() + 1];
        for (int r = 0; r < taskData.length; r++) {
            for (int c = 0; c < coltbl.size(); c++)
                newData[r][c] = taskData[r][c];
        }
        taskData = newData;
        int num = coltbl.size();
        coltbl.put(name, num);
    }

    public boolean isModified() {
        return modified;
    }

    protected void setModified() {
        this.modified = true;
    }
}

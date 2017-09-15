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

public class CustomTask extends ApplicationTask {

    public CustomTask(String taskName, String[] columns) {
        super(taskName, columns);
    }

    public CustomTask(String taskName, String[][] taskData) {
        super(taskName, taskData);
    }

    public void setItem(String columnName, int row, String value) {
        super.setItem(columnName, row, value);
    }

    public String getItem(String columnName, int row) {
        modified = true;
        return super.getItem(columnName, row);
    }

}

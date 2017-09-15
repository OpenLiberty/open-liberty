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

import com.ibm.websphere.simplicity.application.AppConstants;

public class CorrectOracleIsolationLevelTask extends MultiEntryApplicationTask {

    public CorrectOracleIsolationLevelTask() {

    }

    public CorrectOracleIsolationLevelTask(String[][] taskData) {
        super(AppConstants.CorrectOracleIsolationLevelTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new CorrectOracleIsolationLevelEntry(data, this));
        }
    }

    public CorrectOracleIsolationLevelTask(String[] columns) {
        super(AppConstants.CorrectOracleIsolationLevelTask, columns);
    }

    @Override
    public CorrectOracleIsolationLevelEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (CorrectOracleIsolationLevelEntry) entries.get(i);
    }

    /*
     * Need more info about this task. What's the likely uniqueness scenario
     * for the columns? That'll help define generalized setters & getters.
     */

}

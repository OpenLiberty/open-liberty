/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jbatch.utility.utils;

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.jbatch.utility.JBatchUtilityTask;

/**
 * 
 */
public class TaskList extends ArrayList<JBatchUtilityTask> {
    

    /**
     * 
     * @param taskName desired task name
     * 
     * @return the JBatchUtilityTask with that name, or null if no match is found
     */
    public JBatchUtilityTask forName(String taskName) {
        for (JBatchUtilityTask task : this) {
            if (task.getTaskName().equals(taskName)) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * @return the list of task names.
     */
    public List<String> getTaskNames() {
        List<String> retMe = new ArrayList<String>();
        for (JBatchUtilityTask task : this) {
            retMe.add( task.getTaskName() );
        }
        return retMe;
    }

}

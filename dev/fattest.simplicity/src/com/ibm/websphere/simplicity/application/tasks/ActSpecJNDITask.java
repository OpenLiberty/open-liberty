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

public class ActSpecJNDITask extends MultiEntryApplicationTask {

    public ActSpecJNDITask() {}

    public ActSpecJNDITask(String[][] taskData) {
        super(AppConstants.ActSpecJNDITask, taskData);
        for (String[] data : taskData) {
            this.entries.add(new ActSpecJNDIEntry(data, this));
        }
    }

    public ActSpecJNDITask(String[] columns) {
        super(AppConstants.ActSpecJNDITask, columns);
    }

    @Override
    public ActSpecJNDIEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (ActSpecJNDIEntry) entries.get(i);
    }

}

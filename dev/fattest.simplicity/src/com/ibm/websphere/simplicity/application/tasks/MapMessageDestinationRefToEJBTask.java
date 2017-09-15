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

public class MapMessageDestinationRefToEJBTask extends MultiEntryApplicationTask {

    public MapMessageDestinationRefToEJBTask() {

    }

    public MapMessageDestinationRefToEJBTask(String[][] taskData) {
        super(AppConstants.MapMessageDestinationRefToEJBTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapMessageDestinationRefToEJBEntry(data, this));
        }
    }

    public MapMessageDestinationRefToEJBTask(String[] columns) {
        super(AppConstants.MapMessageDestinationRefToEJBTask, columns);
    }

    @Override
    public MapMessageDestinationRefToEJBEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapMessageDestinationRefToEJBEntry) entries.get(i);
    }

}

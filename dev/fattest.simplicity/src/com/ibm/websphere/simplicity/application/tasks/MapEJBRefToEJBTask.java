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

public class MapEJBRefToEJBTask extends MultiEntryApplicationTask {

    public MapEJBRefToEJBTask() {

    }

    public MapEJBRefToEJBTask(String[][] taskData) {
        super(AppConstants.MapEJBRefToEJBTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapEJBRefToEJBEntry(data, this));
        }
    }

    public MapEJBRefToEJBTask(String[] columns) {
        super(AppConstants.MapEJBRefToEJBTask, columns);
    }

    @Override
    public MapEJBRefToEJBEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapEJBRefToEJBEntry) entries.get(i);
    }

}

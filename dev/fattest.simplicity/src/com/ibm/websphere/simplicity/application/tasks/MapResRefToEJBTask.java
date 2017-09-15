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

import java.util.List;

import com.ibm.websphere.simplicity.application.AppConstants;
import com.ibm.websphere.simplicity.application.AssetModule;

public class MapResRefToEJBTask extends MultiEntryApplicationTask {

    public MapResRefToEJBTask() {

    }

    public MapResRefToEJBTask(String[][] taskData) {
        super(AppConstants.MapResRefToEJBTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapResRefToEJBEntry(data, this));
        }
    }

    public MapResRefToEJBTask(String[] columns) {
        super(AppConstants.MapResRefToEJBTask, columns);
    }

    @Override
    public MapResRefToEJBEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapResRefToEJBEntry) entries.get(i);
    }

    @SuppressWarnings("unchecked")
    public List<MapResRefToEJBEntry> getResourceReferenceToResourceMappings(AssetModule module) {
        return (List<MapResRefToEJBEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }
}

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
import com.ibm.websphere.simplicity.application.AssetModule;
import com.ibm.websphere.simplicity.exception.TaskEntryNotFoundException;

public class BackendIdSelectionTask extends MultiEntryApplicationTask {

    public BackendIdSelectionTask() {

    }

    public BackendIdSelectionTask(String[][] taskData) {
        super(AppConstants.BackendIdSelectionTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new BackendIdSelectionEntry(data, this));
        }
    }

    public BackendIdSelectionTask(String[] columns) {
        super(AppConstants.BackendIdSelectionTask, columns);
    }

    @Override
    public BackendIdSelectionEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (BackendIdSelectionEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public String getBackendIdForModule(AssetModule module) {

        BackendIdSelectionEntry entry = (BackendIdSelectionEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        return (entry != null ? entry.getCurrentBackendId() : null);
    }

    public void setBackendIdForModule(AssetModule module, String backendId) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        BackendIdSelectionEntry entry = (BackendIdSelectionEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setCurrentBackendId(backendId);
    }

}

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

public class CtxRootForWebModTask extends MultiEntryApplicationTask {

    public CtxRootForWebModTask() {

    }

    public CtxRootForWebModTask(String[][] taskData) {
        super(AppConstants.CtxRootForWebMethodTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new CtxRootForWebModEntry(data, this));
        }
    }

    public CtxRootForWebModTask(String[] columns) {
        super(AppConstants.CtxRootForWebMethodTask, columns);
    }

    @Override
    public CtxRootForWebModEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (CtxRootForWebModEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public String getContextRoot(AssetModule module) {
        CtxRootForWebModEntry entry = (CtxRootForWebModEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        return entry.getContextRoot();
    }

    public void setContextRoot(AssetModule module, String contextRoot) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        CtxRootForWebModEntry entry = (CtxRootForWebModEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setContextRoot(contextRoot);
    }

}

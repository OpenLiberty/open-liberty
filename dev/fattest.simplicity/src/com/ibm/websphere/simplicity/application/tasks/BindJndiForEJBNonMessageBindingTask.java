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

public class BindJndiForEJBNonMessageBindingTask extends MultiEntryApplicationTask {

    public BindJndiForEJBNonMessageBindingTask() {

    }

    public BindJndiForEJBNonMessageBindingTask(String[][] taskData) {
        super(AppConstants.BindJndiForEJBNonMessageBindingTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new BindJndiForEJBNonMessageBindingEntry(data, this));
        }
    }

    public BindJndiForEJBNonMessageBindingTask(String[] columns) {
        super(AppConstants.BindJndiForEJBNonMessageBindingTask, columns);
    }

    @Override
    public BindJndiForEJBNonMessageBindingEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (BindJndiForEJBNonMessageBindingEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public BindJndiForEJBNonMessageBindingEntry getBindingsForModule(AssetModule module) {
        return (BindJndiForEJBNonMessageBindingEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
    }

    public void setBindingsForModule(AssetModule module, String globalJndi, String localHomeJndi, String remoteHomeJndi) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        this.modified = true;
        BindJndiForEJBNonMessageBindingEntry entry = getBindingsForModule(module);
        entry.setJndi(globalJndi);
        entry.setLocalHomeJndi(localHomeJndi);
        entry.setRemoteHomeJndi(remoteHomeJndi);
    }

}

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

public class EnsureMethodProtectionFor10EJBTask extends MultiEntryApplicationTask {

    public EnsureMethodProtectionFor10EJBTask() {

    }

    public EnsureMethodProtectionFor10EJBTask(String[][] taskData) {
        super(AppConstants.EnsureMethodProtectionFor10EJBTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new EnsureMethodProtectionFor10EJBEntry(data, this));
        }
    }

    public EnsureMethodProtectionFor10EJBTask(String[] columns) {
        super(AppConstants.EnsureMethodProtectionFor10EJBTask, columns);
    }

    @Override
    public EnsureMethodProtectionFor10EJBEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (EnsureMethodProtectionFor10EJBEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public boolean getDenyAllAccess(AssetModule module) {
        EnsureMethodProtectionFor10EJBEntry entry = (EnsureMethodProtectionFor10EJBEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        return entry.getDenyAll();
    }

    public void setDenyAllAccess(AssetModule module, boolean denyAll) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        EnsureMethodProtectionFor10EJBEntry entry = (EnsureMethodProtectionFor10EJBEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setDenyAll(denyAll);
    }

}

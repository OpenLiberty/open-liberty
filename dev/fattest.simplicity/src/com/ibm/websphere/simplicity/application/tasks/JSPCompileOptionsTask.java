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

public class JSPCompileOptionsTask extends MultiEntryApplicationTask {

    public JSPCompileOptionsTask() {

    }

    public JSPCompileOptionsTask(String[][] taskData) {
        super(AppConstants.JSPCompileOptionsTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new JSPCompileOptionsEntry(data, this));
        }
    }

    public JSPCompileOptionsTask(String[] columns) {
        super(AppConstants.JSPCompileOptionsTask, columns);
    }

    @Override
    public JSPCompileOptionsEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (JSPCompileOptionsEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public JSPCompileOptionsEntry getCompileOptions(AssetModule module) {
        return (JSPCompileOptionsEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
    }

    public void setCompileOptions(AssetModule module, String classPath, boolean useFullPackageNames, String sourceLevel, boolean disableRuntimeCompilation) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        JSPCompileOptionsEntry entry = (JSPCompileOptionsEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setJspClasspath(classPath);
        entry.setUseFullPackageNames(useFullPackageNames);
        entry.setSourceLevel(sourceLevel);
        entry.setDisableRuntimeCompilation(disableRuntimeCompilation);
    }

}

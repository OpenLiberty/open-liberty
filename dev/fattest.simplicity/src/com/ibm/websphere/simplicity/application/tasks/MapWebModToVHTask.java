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

public class MapWebModToVHTask extends MultiEntryApplicationTask {

    public MapWebModToVHTask() {

    }

    public MapWebModToVHTask(String[][] taskData) {
        super(AppConstants.MapWebModToVHTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapWebModToVHEntry(data, this));
        }
    }

    public MapWebModToVHTask(String[] columns) {
        super(AppConstants.MapWebModToVHTask, columns);
    }

    @Override
    public MapWebModToVHEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapWebModToVHEntry) entries.get(i);
    }

    /**
     * Determines whether the specified module exists in this application task.
     * 
     * @param module The module to find.
     * @return True if the module is found in this application task.
     */
    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    /**
     * Retrieves the currently configured virtual host for the specified module.
     * 
     * @param module The module whose virtual host will be retrieved.
     * @return The configured virtual host for the module, or null if either the module doesn't exist or the virtual host has not been configured.
     */
    public String getVirtualHost(AssetModule module) {
        MapWebModToVHEntry entry = (MapWebModToVHEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        return (entry != null ? entry.getVirtualHost() : null);
    }

    /**
     * Sets the virtual host for the specified module only.
     * 
     * @param module The module whose virtual host configuration will be changed.
     * @param virtualHost The target virtual host.
     * @throws Exception
     */
    public void setVirtualHost(AssetModule module, String virtualHost) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        MapWebModToVHEntry entry = (MapWebModToVHEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setVirtualHost(virtualHost);
    }

    /**
     * Sets the virtual host for all modules. Note that this method does not distinguish
     * between existing null and non-null virtual host values.
     * 
     * @param virtualHost The virtual host to which all modules will be mapped.
     * @throws Exception
     */
    public void setVirtualHost(String virtualHost) throws Exception {
        modified = true;
        for (int i = 0; i < size(); i++) {
            get(i).setVirtualHost(virtualHost);
        }
    }

}

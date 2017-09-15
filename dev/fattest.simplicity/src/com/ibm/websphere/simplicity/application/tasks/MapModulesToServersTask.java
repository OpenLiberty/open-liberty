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

import com.ibm.websphere.simplicity.Scope;
import com.ibm.websphere.simplicity.application.AppConstants;
import com.ibm.websphere.simplicity.application.AssetModule;
import com.ibm.websphere.simplicity.exception.TaskEntryNotFoundException;

public class MapModulesToServersTask extends MultiEntryApplicationTask {

    public MapModulesToServersTask() {

    }

    public MapModulesToServersTask(String[][] taskData) {
        super(AppConstants.MapModulesToServersTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new MapModulesToServersEntry(data, this));
        }
    }

    public MapModulesToServersTask(String[] columns) {
        super(AppConstants.MapModulesToServersTask, columns);
    }

    @Override
    public MapModulesToServersEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (MapModulesToServersEntry) entries.get(i);
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
     * @param module The module whose target will be retrieved.
     * @return The raw target string.
     */
    public String getTarget(AssetModule module) {
        String find = module.getURI();
        MapModulesToServersEntry entry = (MapModulesToServersEntry) getEntry(AppConstants.APPDEPL_URI, find);
        if (entry == null) {
            // Sometimes WAS gives us slightly differing formats
            if (find.indexOf("+") != -1)
                find = find.replace("+", ",");
            else
                find = find.replace(",", "+");
            entry = (MapModulesToServersEntry) getEntry(AppConstants.APPDEPL_URI, find);
        }
        if (entry == null)
            return null;
        else
            return entry.getTarget();
    }

    /**
     * Sets the server or cluster to which the module will be deployed.
     * 
     * @param module The module to be deployed.
     * @param target The server or cluster to which the module will be deployed.
     * @throws Exception
     */
    public void setTarget(AssetModule module, Scope target) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        MapModulesToServersEntry entry = (MapModulesToServersEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setTarget(target);
    }

    /**
     * Sets the servers and/or clusters to which the module will be deployed.
     * 
     * @param module The module to be deployed.
     * @param targets The servers and/or clusters to which the module will be deployed.
     * @throws Exception
     */
    public void setTarget(AssetModule module, List<Scope> targets) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        MapModulesToServersEntry entry = (MapModulesToServersEntry) getEntry(AppConstants.APPDEPL_URI, module.getURI());
        entry.setTargets(targets);
    }

    /**
     * Sets all modules to deploy on the specified server or cluster.
     * 
     * @param target The server or cluster to which all modules will be deployed.
     * @throws Exception
     */
    public void setTarget(Scope target) throws Exception {
        modified = true;
        for (int i = 0; i < size(); i++) {
            get(i).setTarget(target);
        }
    }

    /**
     * Sets all modules to deploy on the specified servers and/or clusters.
     * 
     * @param target The servers and/or clusters to which all modules will be deployed.
     * @throws Exception
     */
    public void setTarget(List<Scope> targets) throws Exception {
        modified = true;
        for (int i = 0; i < size(); i++) {
            get(i).setTargets(targets);
        }
    }

}

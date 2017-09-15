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
import com.ibm.websphere.simplicity.exception.TaskEntryNotFoundException;

public class DataSourceFor20CMPBeansTask extends MultiEntryApplicationTask {

    public DataSourceFor20CMPBeansTask() {

    }

    public DataSourceFor20CMPBeansTask(String[][] taskData) {
        super(AppConstants.DataSourceFor20CMPBeansTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new DataSourceFor20CMPBeansEntry(data, this));
        }
    }

    public DataSourceFor20CMPBeansTask(String[] columns) {
        super(AppConstants.DataSourceFor20CMPBeansTask, columns);
    }

    @Override
    public DataSourceFor20CMPBeansEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (DataSourceFor20CMPBeansEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public boolean hasDataSource(AssetModule module, String ejb) {
        return getEntry(
                        new String[] {
                                      AppConstants.APPDEPL_URI,
                                      AppConstants.APPDEPL_EJB,
                },
                        new String[] {
                                      module.getURI(),
                                      ejb,
                }) != null;
    }

    public DataSourceFor20CMPBeansEntry getDataSource(AssetModule module, String ejb) {
        return (DataSourceFor20CMPBeansEntry) getEntry(
                                                       new String[] {
                                                                     AppConstants.APPDEPL_URI,
                                                                     AppConstants.APPDEPL_EJB,
                },
                                                       new String[] {
                                                                     module.getURI(),
                                                                     ejb,
                });
    }

    public List<DataSourceFor20CMPBeansEntry> getDataSources(AssetModule module) {
        return (List<DataSourceFor20CMPBeansEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }

    public void setDataSource(AssetModule module, String ejb, String jndi, String resourceAuth, String loginConfig, String authorizationProps) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        DataSourceFor20CMPBeansEntry entry = getDataSource(module, ejb);
        if (entry == null) {
            // We know we have one for the module, so get a template
            DataSourceFor20CMPBeansEntry other = getDataSources(module).get(0);
            entry = new DataSourceFor20CMPBeansEntry(new String[coltbl.size()], this);
            entry.setAppVersion(other.getAppVersion());
            entry.setEjbModule(other.getEjbModule());
            entry.setUri(other.getUri());
            entry.setEjbVersion(other.getEjbVersion());
            entry.setEjb(ejb);
        }
        entry.setJndi(jndi);
        entry.setResourceAuth(resourceAuth);
        entry.setLoginConfig(loginConfig);
        entry.setAuthorizationProps(authorizationProps);
    }

}

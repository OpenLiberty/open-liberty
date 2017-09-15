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

public class DataSourceFor20EJBModulesTask extends MultiEntryApplicationTask {

    public DataSourceFor20EJBModulesTask() {

    }

    public DataSourceFor20EJBModulesTask(String[][] taskData) {
        super(AppConstants.DataSourceFor20EJBModulesTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new DataSourceFor20EJBModulesEntry(data, this));
        }
    }

    public DataSourceFor20EJBModulesTask(String[] columns) {
        super(AppConstants.DataSourceFor20EJBModulesTask, columns);
    }

    @Override
    public DataSourceFor20EJBModulesEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (DataSourceFor20EJBModulesEntry) entries.get(i);
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

    public void deleteDataSource(AssetModule module, String ejb) {
        DataSourceFor20EJBModulesEntry entry = getDataSource(module, ejb);
        if (entry != null)
            entry.deleteEntry();
    }

    public List<DataSourceFor20EJBModulesEntry> getDataSources(AssetModule module) {
        return (List<DataSourceFor20EJBModulesEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }

    public DataSourceFor20EJBModulesEntry getDataSource(AssetModule module, String ejb) {
        return (DataSourceFor20EJBModulesEntry) getEntry(
                                                         new String[] {
                                                                       AppConstants.APPDEPL_URI,
                                                                       AppConstants.APPDEPL_EJB,
                },
                                                         new String[] {
                                                                       module.getURI(),
                                                                       ejb,
                });
    }

    public void setDataSource(AssetModule module, String ejb, String jndi, String resourceAuth, String loginConfig, String authorizationProps, String dataSourceProps) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        DataSourceFor20EJBModulesEntry entry = getDataSource(module, ejb);
        if (entry == null) {
            // We know we have one for the module, so get a template
            DataSourceFor20EJBModulesEntry other = getDataSources(module).get(0);
            entry = new DataSourceFor20EJBModulesEntry(new String[coltbl.size()], this);
            entry.setAppVersion(other.getAppVersion());
            entry.setEjbModule(other.getEjbModule());
            entry.setUri(other.getUri());
        }
        entry.setJndi(jndi);
        entry.setResourceAuth(resourceAuth);
        entry.setLoginConfig(loginConfig);
        entry.setAuthorizationProps(authorizationProps);
        entry.setDataSourceProps(dataSourceProps);
    }

}

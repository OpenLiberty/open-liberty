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

public class DataSourceFor10EJBModulesTask extends MultiEntryApplicationTask {

    public DataSourceFor10EJBModulesTask() {

    }

    public DataSourceFor10EJBModulesTask(String[][] taskData) {
        super(AppConstants.DataSourceFor10EJBModulesTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new DataSourceFor10EJBModulesEntry(data, this));
        }
    }

    public DataSourceFor10EJBModulesTask(String[] columns) {
        super(AppConstants.DataSourceFor10EJBModulesTask, columns);
    }

    @Override
    public DataSourceFor10EJBModulesEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (DataSourceFor10EJBModulesEntry) entries.get(i);
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

    public DataSourceFor10EJBModulesEntry getDataSource(AssetModule module, String ejb) {
        return (DataSourceFor10EJBModulesEntry) getEntry(
                                                         new String[] {
                                                                       AppConstants.APPDEPL_URI,
                                                                       AppConstants.APPDEPL_EJB,
                },
                                                         new String[] {
                                                                       module.getURI(),
                                                                       ejb,
                });
    }

    public List<DataSourceFor10EJBModulesEntry> getDataSources(AssetModule module) {
        return (List<DataSourceFor10EJBModulesEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }

    public void setDataSource(AssetModule module, String ejb, String jndi, String user, String password, String loginConfig, String authorizationProps) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        DataSourceFor10EJBModulesEntry entry = getDataSource(module, ejb);
        if (entry == null) {
            DataSourceFor10EJBModulesEntry other = getDataSources(module).get(0);
            entry = new DataSourceFor10EJBModulesEntry(new String[coltbl.size()], this);
            entry.setAppVersion(other.getAppVersion());
            entry.setEjbModule(other.getEjbModule());
            entry.setUri(other.getUri());
        }
        entry.setJndi(jndi);
        entry.setUser(user);
        entry.setPassword(password);
        entry.setLoginConfig(loginConfig);
        entry.setAuthorizationProps(authorizationProps);
    }

}

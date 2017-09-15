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

public class DataSourceFor10CMPBeansTask extends MultiEntryApplicationTask {

    public DataSourceFor10CMPBeansTask() {

    }

    public DataSourceFor10CMPBeansTask(String[][] taskData) {
        super(AppConstants.DataSourceFor10CMPBeansTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new DataSourceFor10CMPBeansEntry(data, this));
        }
    }

    public DataSourceFor10CMPBeansTask(String[] columns) {
        super(AppConstants.DataSourceFor10CMPBeansTask, columns);
    }

    @Override
    public DataSourceFor10CMPBeansEntry get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (DataSourceFor10CMPBeansEntry) entries.get(i);
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

    public DataSourceFor10CMPBeansEntry getDataSource(AssetModule module, String ejb) {
        return (DataSourceFor10CMPBeansEntry) getEntry(
                                                       new String[] {
                                                                     AppConstants.APPDEPL_URI,
                                                                     AppConstants.APPDEPL_EJB,
                },
                                                       new String[] {
                                                                     module.getURI(),
                                                                     ejb,
                });
    }

    public List<DataSourceFor10CMPBeansEntry> getDataSources(AssetModule module) {
        return (List<DataSourceFor10CMPBeansEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }

    public void setDataSource(AssetModule module, String ejb, String jndi, String user, String password, String loginConfig, String authorizationProps) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        modified = true;
        DataSourceFor10CMPBeansEntry entry = getDataSource(module, ejb);
        if (entry == null) {
            DataSourceFor10CMPBeansEntry other = getDataSources(module).get(0);
            entry = new DataSourceFor10CMPBeansEntry(new String[coltbl.size()], this);
            entry.setEjbModule(other.getEjbModule());
            entry.setUri(other.getUri());
            entry.setEjb(ejb);
        }
        entry.setJndi(jndi);
        entry.setUser(user);
        entry.setPassword(password);
        entry.setLoginConfig(loginConfig);
        entry.setAuthorizationProps(authorizationProps);
    }

}

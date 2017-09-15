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

public class CorrectUseSystemIdentityTask extends MultiEntryApplicationTask {

    public CorrectUseSystemIdentityTask() {

    }

    public CorrectUseSystemIdentityTask(String[][] taskData) {
        super(AppConstants.CorrectUseSystemIdentityTask, taskData);
        for (int i = 1; i < taskData.length; i++) {
            String[] data = taskData[i];
            this.entries.add(new CorrectUseSystemIdentityEntry(data, this));
        }
    }

    public CorrectUseSystemIdentityTask(String[] columns) {
        super(AppConstants.CorrectUseSystemIdentityTask, columns);
    }

    @Override
    public Object get(int i) {
        if (i >= size())
            throw new ArrayIndexOutOfBoundsException(i);
        return (CorrectUseSystemIdentityEntry) entries.get(i);
    }

    public boolean hasModule(AssetModule module) {
        return getEntry(AppConstants.APPDEPL_URI, module.getURI()) != null;
    }

    public boolean hasIdentity(AssetModule module, String ejb, String methodSignature) {
        return getEntry(
                        new String[] {
                                      AppConstants.APPDEPL_URI,
                                      AppConstants.APPDEPL_EJB,
                                      AppConstants.APPDEPL_METHOD_SIGNATURE
                },
                        new String[] {
                                      module.getURI(),
                                      ejb,
                                      methodSignature
                }) != null;
    }

    public List<CorrectUseSystemIdentityEntry> getIdentities(AssetModule module) {
        return (List<CorrectUseSystemIdentityEntry>) getEntries(AppConstants.APPDEPL_URI, module.getURI());
    }

    public List<CorrectUseSystemIdentityEntry> getIdentities(AssetModule module, String ejb) {
        return (List<CorrectUseSystemIdentityEntry>) getEntries(
                                                                new String[] {
                                                                              AppConstants.APPDEPL_URI,
                                                                              AppConstants.APPDEPL_EJB
                },
                                                                new String[] {
                                                                              module.getURI(),
                                                                              ejb
                });
    }

    public CorrectUseSystemIdentityEntry getIdentity(AssetModule module, String ejb, String methodSignature) throws Exception {
        List<CorrectUseSystemIdentityEntry> entries = getIdentities(module, ejb);
        for (CorrectUseSystemIdentityEntry entry : entries)
            if (entry.getMethodSignature().equals(methodSignature))
                return entry;
        return null;
    }

    /**
     * Sets the role, user, and password for the specified method in the specified
     * EJB
     * 
     * @param module
     * @param methodSignature
     * @param role
     * @param user
     * @param password
     * @throws Exception
     */
    public void setIdentity(AssetModule module, String ejb, String methodSignature, String role, String user, String password) throws Exception {
        if (!hasModule(module))
            throw new TaskEntryNotFoundException();
        this.modified = true;
        CorrectUseSystemIdentityEntry entry = getIdentity(module, ejb, methodSignature);
        if (entry == null) {
            // We know we have the module, so get a template entry
            CorrectUseSystemIdentityEntry other = getIdentities(module).get(0);
            entry = new CorrectUseSystemIdentityEntry(new String[coltbl.size()], this);
            entry.setEjbModule(other.getEjbModule());
            entry.setUri(other.getUri());
            entry.setEjb(ejb);
            entry.setMethodSignature(methodSignature);
        }
        entry.setRole(role);
        entry.setUser(user);
        entry.setPassword(password);
    }

}

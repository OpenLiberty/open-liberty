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

public class BackendIdSelectionEntry extends TaskEntry {

    public BackendIdSelectionEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getAppVersion() throws Exception {
        return super.getAppVersion();
    }

    protected void setAppVersion(String value) throws Exception {
        super.setAppVersion(value);
    }

    public String getModuleVersion() throws Exception {
        return super.getModuleVersion();
    }

    protected void setModuleVersion(String value) throws Exception {
        super.setModuleVersion(value);
    }

    public String getEjbModule() {
        return super.getEjbModule();
    }

    protected void setEjbModule(String value) {
        super.setEjbModule(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getBackendIds() {
        return getString(AppConstants.APPDEPL_BACKENDIDS);
    }

    protected void setBackendIds(String value) {
        setItem(AppConstants.APPDEPL_BACKENDIDS, value);
    }

    public String getCurrentBackendId() {
        return getString(AppConstants.APPDEPL_CURRENT_BACKEND_ID);
    }

    public void setCurrentBackendId(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_CURRENT_BACKEND_ID, value);
    }

}

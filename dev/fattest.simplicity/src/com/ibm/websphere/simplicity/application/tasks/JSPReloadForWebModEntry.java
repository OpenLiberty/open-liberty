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

public class JSPReloadForWebModEntry extends TaskEntry {

    public JSPReloadForWebModEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getWebModule() {
        return super.getWebModule();
    }

    protected void setWebModule(String value) {
        super.setWebModule(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public boolean getJspReload() {
        return getBoolean(AppConstants.APPDEPL_JSP_RELOADENABLED, AppConstants.APPDEPL_JSP_RELOADENABLED_DEFAULT);
    }

    public void setJspReload(boolean value) {
        task.setModified();
        setBoolean(AppConstants.APPDEPL_JSP_RELOADENABLED, value);
    }

    public int getJspReloadInterval() {
        return getInteger(AppConstants.APPDEPL_JSP_RELOADINTERVAL, AppConstants.APPDEPL_JSP_RELOADINTERVAL_DEFAULT);
    }

    public void setJspReloadInterval(int value) {
        task.setModified();
        setInteger(AppConstants.APPDEPL_JSP_RELOADINTERVAL, value);
    }

}

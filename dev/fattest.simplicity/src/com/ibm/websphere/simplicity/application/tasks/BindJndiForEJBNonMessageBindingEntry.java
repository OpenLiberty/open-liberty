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

public class BindJndiForEJBNonMessageBindingEntry extends TaskEntry {

    public BindJndiForEJBNonMessageBindingEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getEjbModule() {
        return super.getEjbModule();
    }

    protected void setEjbModule(String value) {
        super.setEjbModule(value);
    }

    public String getEjb() {
        return super.getEjb();
    }

    protected void setEjb(String value) {
        super.setEjb(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getJndi() throws Exception {
        return super.getJndi();
    }

    public void setJndi(String value) throws Exception {
        task.setModified();
        super.setJndi(value);
    }

    public String getModuleVersion() throws Exception {
        hasAtLeast(5);
        return super.getModuleVersion();
    }

    protected void setModuleVersion(String value) throws Exception {
        hasAtLeast(5);
        super.setModuleVersion(value);
    }

    public String getLocalHomeJndi() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_EJB_LOCAL_HOME_JNDI);
    }

    public void setLocalHomeJndi(String value) throws Exception {
        hasAtLeast(5);
        task.setModified();
        setItem(AppConstants.APPDEPL_EJB_LOCAL_HOME_JNDI, value);
    }

    public String getRemoteHomeJndi() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_EJB_REMOTE_HOME_JNDI);
    }

    public void setRemoteHomeJndi(String value) throws Exception {
        hasAtLeast(5);
        task.setModified();
        setItem(AppConstants.APPDEPL_EJB_REMOTE_HOME_JNDI, value);
    }

}

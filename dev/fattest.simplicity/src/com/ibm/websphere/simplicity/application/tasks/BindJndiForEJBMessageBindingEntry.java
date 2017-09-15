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

public class BindJndiForEJBMessageBindingEntry extends TaskEntry {

    public BindJndiForEJBMessageBindingEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getEjb() {
        return super.getEjb();
    }

    protected void setEjb(String value) {
        super.setEjb(value);
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

    public Integer getListenerPort() {
        return getInteger(AppConstants.APPDEPL_LISTENER_PORT);
    }

    public void setListenerPort(int value) {
        task.setModified();
        setInteger(AppConstants.APPDEPL_LISTENER_PORT, value);
    }

    public String getJndi() throws Exception {
        hasAtLeast(5);
        return super.getJndi();
    }

    public void setJndiName(String value) throws Exception {
        hasAtLeast(5);
        task.setModified();
        super.setJndi(value);
    }

    public String getAppVersion() throws Exception {
        hasAtLeast(5);
        return super.getAppVersion();
    }

    protected void setAppVersion(String value) throws Exception {
        hasAtLeast(5);
        super.setAppVersion(value);
    }

    public String getModuleVersion() throws Exception {
        hasAtLeast(5);
        return super.getModuleVersion();
    }

    protected void setModuleVersion(String value) throws Exception {
        hasAtLeast(5);
        super.setModuleVersion(value);
    }

    public String getJndiDestination() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_JNDI_DEST);
    }

    public void setJndiDestination(String value) throws Exception {
        hasAtLeast(5);
        task.setModified();
        setItem(AppConstants.APPDEPL_JNDI_DEST, value);
    }

    public String getActSpecAuth() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_ACT_AUTH);
    }

    public void setActSpecAuth(String value) throws Exception {
        hasAtLeast(5);
        task.setModified();
        setItem(AppConstants.APPDEPL_ACT_AUTH, value);
    }

    public String getMessagingType() throws Exception {
        hasAtLeast(5);
        return getString(AppConstants.APPDEPL_MESSAGING_TYPE);
    }

    protected void setMessagingType(String value) throws Exception {
        hasAtLeast(5);
        setItem(AppConstants.APPDEPL_MESSAGING_TYPE, value);
    }

}

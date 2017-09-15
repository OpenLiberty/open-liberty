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

public class DataSourceFor10EJBModulesEntry extends TaskEntry {

    public DataSourceFor10EJBModulesEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getAppVersion() throws Exception {
        return super.getAppVersion();
    }

    protected void setAppVersion(String value) throws Exception {
        super.setAppVersion(value);
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

    public String getJndi() throws Exception {
        return super.getJndi();
    }

    public void setJndi(String value) throws Exception {
        task.setModified();
        super.setJndi(value);
    }

    public String getUser() {
        return super.getUser();
    }

    public void setUser(String value) {
        task.setModified();
        super.setUser(value);
    }

    public String getPassword() {
        return super.getPassword();
    }

    public void setPassword(String value) {
        task.setModified();
        super.setPassword(value);
    }

    public String getLoginConfig() throws Exception {
        hasAtLeast(6);
        return super.getLoginConfig();
    }

    public void setLoginConfig(String value) throws Exception {
        hasAtLeast(6);
        task.setModified();
        super.setLoginConfig(value);
    }

    public String getAuthorizationProps() throws Exception {
        hasAtLeast(6);
        return super.getAuthorizationProps();
    }

    public void setAuthorizationProps(String value) throws Exception {
        hasAtLeast(6);
        task.setModified();
        super.setAuthorizationProps(value);
    }

}

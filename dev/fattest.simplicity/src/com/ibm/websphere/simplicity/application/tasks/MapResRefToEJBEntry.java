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

public class MapResRefToEJBEntry extends TaskEntry {

    public MapResRefToEJBEntry(String[] data, MultiEntryApplicationTask task) {
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

    public String getModule() {
        return super.getModule();
    }

    protected void setModule(String value) {
        super.setModule(value);
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

    public String getReferenceBinding() {
        return super.getReferenceBinding();
    }

    protected void setReferenceBinding(String value) {
        super.setReferenceBinding(value);
    }

    public String getType() {
        return getString(AppConstants.APPDEPL_RESREF_TYPE);
    }

    protected void setType(String value) {
        setItem(AppConstants.APPDEPL_RESENVREF_TYPE, value);
    }

    public String getOracleRef() {
        return getString(AppConstants.APPDEPL_ORACLE_REF);
    }

    public void setOracleRef(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_ORACLE_REF, value);
    }

    public String getJndi() throws Exception {
        return super.getJndi();
    }

    public void setJndi(String value) throws Exception {
        task.setModified();
        super.setJndi(value);
    }

    public String getLoginConfig() throws Exception {
        hasAtLeast(10);
        return super.getLoginConfig();
    }

    public void setLoginConfig(String value) throws Exception {
        hasAtLeast(10);
        task.setModified();
        super.setLoginConfig(value);
    }

    public String getAuthorizationProps() throws Exception {
        hasAtLeast(10);
        return super.getAuthorizationProps();
    }

    public void setAuthorizationProps(String value) throws Exception {
        hasAtLeast(10);
        task.setModified();
        super.setAuthorizationProps(value);
    }

    public String getResourceAuth() throws Exception {
        hasAtLeast(10);
        return super.getResourceAuth();
    }

    protected void setResourceAuth(String value) throws Exception {
        hasAtLeast(10);
        super.setResourceAuth(value);
    }

    public String getResDataSourceProps() throws Exception {
        hasAtLeast(13);
        return getString(AppConstants.APPDEPL_DATASOURCE_PROPS);
    }

    public void setResDataSourceProps(String value) throws Exception {
        hasAtLeast(13);
        task.setModified();
        setItem(AppConstants.APPDEPL_DATASOURCE_PROPS, value);
    }

}

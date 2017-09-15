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
import com.ibm.websphere.simplicity.exception.IncompatibleVersionException;

public abstract class TaskEntry {

    private String[] data;
    protected MultiEntryApplicationTask task;

    public TaskEntry(String[] data, MultiEntryApplicationTask task) {
        this.data = data;
        this.task = task;
    }

    public String[] getTaskData() {
        return data;
    }

    public void deleteEntry() {
        task.delete(this);
    }

    protected void setBoolean(String columnName, boolean value) {
        setItem(columnName, value ? AppConstants.YES_KEY : AppConstants.NO_KEY);
    }

    protected void setBooleanTrueFalse(String columnName, boolean value) {
        setItem(columnName, value ? "true" : "false");
    }

    protected void setYesNo(String columnName, boolean value) {
        setItem(columnName, value ? "Yes" : "No");
    }

    protected void setInteger(String columnName, int value) {
        setItem(columnName, Integer.toString(value));
    }

    protected void setItem(String columnName, String value) {
        Integer col = task.coltbl.get(columnName);
        if (col != null)
            data[col] = value;
    }

    protected boolean getBoolean(String columnName) {
        return getBoolean(columnName, false);
    }

    protected boolean getBoolean(String columnName, Boolean deflt) {
        String s = getItem(columnName);
        boolean b = false;
        if (s != null) {
            b = (s.equalsIgnoreCase(AppConstants.YES_KEY) || s.equalsIgnoreCase("true") || s.equalsIgnoreCase("yes"));
        }
        return b || deflt;
    }

    protected String getString(String columnName) {
        return getString(columnName, null);
    }

    protected String getString(String columnName, String deflt) {
        String s = getItem(columnName);
        if (s == null)
            s = deflt;
        return s;
    }

    protected Integer getInteger(String columnName) {
        return getInteger(columnName, null);
    }

    protected Integer getInteger(String columnName, Integer deflt) {
        String s = getItem(columnName);
        Integer ret = toInteger(s);
        if (ret == null)
            ret = deflt;
        return ret;
    }

    protected Integer toInteger(String s) {
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException nfe) {
            return null;
        }
    }

    protected String getItem(String columnName) {
        String ret = null;
        Integer col = task.coltbl.get(columnName);
        if (col != null)
            ret = data[col];
        return ret;
    }

    // Earlier versions do not have certain columns
    protected void hasAtLeast(int columns) throws IncompatibleVersionException {
        if (data.length < columns)
            throw new IncompatibleVersionException("");
    }

    /*
     * =====================================================================================
     * The methods below this point are stock methods to avoid a lot of
     * repetition in individual tasks.
     */

    protected String getAppVersion() throws Exception {
        return getString(AppConstants.APPDEPL_APP_VERSION);
    }

    protected String getEjbVersion() throws Exception {
        return getString(AppConstants.APPDEPL_EJB_VERSION);
    }

    protected String getServer() {
        return getString(AppConstants.APPDEPL_SERVER_NAME);
    }

    protected String getModuleVersion() throws Exception {
        return getString(AppConstants.APPDEPL_MODULE_VERSION);
    }

    protected String getEjbModule() {
        return getString(AppConstants.APPDEPL_EJB_MODULE);
    }

    protected String getWebModule() {
        return getString(AppConstants.APPDEPL_WEB_MODULE, null);
    }

    protected String getModule() {
        return getString(AppConstants.APPDEPL_MODULE, null);
    }

    protected String getEjb() {
        return getString(AppConstants.APPDEPL_EJB);
    }

    protected String getUri() {
        return getString(AppConstants.APPDEPL_URI);
    }

    protected String getJndi() throws Exception {
        return getString(AppConstants.APPDEPL_JNDI);
    }

    protected String getUser() {
        return getString(AppConstants.APPDEPL_USERNAME, null);
    }

    protected String getPassword() {
        return getString(AppConstants.APPDEPL_PASSWORD, null);
    }

    protected String getLoginConfig() throws Exception {
        return getString(AppConstants.APPDEPL_LOGIN_CONFIG);
    }

    protected String getAuthorizationProps() throws Exception {
        return getString(AppConstants.APPDEPL_AUTH_PROPS);
    }

    protected String getResEnvRefBinding() {
        return getString(AppConstants.APPDEPL_REFERENCE_BINDING);
    }

    protected String getResEnvRefType() {
        return getString(AppConstants.APPDEPL_RESENVREF_TYPE);
    }

    protected String getReferenceBinding() {
        return getString(AppConstants.APPDEPL_REFERENCE_BINDING);
    }

    protected String getClassName() {
        return getString(AppConstants.APPDEPL_CLASS);
    }

    protected String getRole() {
        return getString(AppConstants.APPDEPL_ROLE, null);
    }

    protected String getWebContextRoot() {
        return getString(AppConstants.APPDEPL_WEB_CONTEXTROOT, null);
    }

    protected String getResourceAuth() throws Exception {
        return getString(AppConstants.APPDEPL_RES_AUTH);
    }

    protected String getRoleUser() {
        return getString(AppConstants.APPDEPL_ROLE_USER);
    }

    protected void setAppVersion(String value) throws Exception {
        setItem(AppConstants.APPDEPL_APP_VERSION, value);
    }

    protected void setEjbVersion(String value) throws Exception {
        setItem(AppConstants.APPDEPL_EJB_VERSION, value);
    }

    protected void setServer(String value) {
        setItem(AppConstants.APPDEPL_SERVER_NAME, value);
    }

    protected void setModuleVersion(String value) throws Exception {
        setItem(AppConstants.APPDEPL_MODULE_VERSION, value);
    }

    protected void setEjbModule(String value) {
        setItem(AppConstants.APPDEPL_EJB_MODULE, value);
    }

    protected void setWebModule(String value) {
        setItem(AppConstants.APPDEPL_WEB_MODULE, value);
    }

    protected void setModule(String value) {
        setItem(AppConstants.APPDEPL_MODULE, value);
    }

    protected void setEjb(String value) {
        setItem(AppConstants.APPDEPL_EJB, value);
    }

    protected void setUri(String value) {
        setItem(AppConstants.APPDEPL_URI, value);
    }

    protected void setJndi(String value) throws Exception {
        setItem(AppConstants.APPDEPL_JNDI, value);
    }

    protected void setUser(String value) {
        setItem(AppConstants.APPDEPL_USERNAME, value);
    }

    protected void setRoleUser(String value) {
        setItem(AppConstants.APPDEPL_ROLE_USER, value);
    }

    protected void setPassword(String value) {
        setItem(AppConstants.APPDEPL_PASSWORD, value);
    }

    protected void setLoginConfig(String value) throws Exception {
        setItem(AppConstants.APPDEPL_LOGIN_CONFIG, value);
    }

    protected void setAuthorizationProps(String value) throws Exception {
        setItem(AppConstants.APPDEPL_AUTH_PROPS, value);
    }

    protected void setResEnvRefBinding(String value) {
        setItem(AppConstants.APPDEPL_REFERENCE_BINDING, value);
    }

    protected void setResEnvRefType(String value) {
        setItem(AppConstants.APPDEPL_RESENVREF_TYPE, value);
    }

    protected void setReferenceBinding(String value) {
        setItem(AppConstants.APPDEPL_REFERENCE_BINDING, value);
    }

    protected void setClassName(String value) {
        setItem(AppConstants.APPDEPL_CLASS, value);
    }

    protected void setRole(String value) {
        setItem(AppConstants.APPDEPL_ROLE, value);
    }

    protected void setWebContextRoot(String value) {
        setItem(AppConstants.APPDEPL_WEB_CONTEXTROOT, value);
    }

    protected void setResourceAuth(String value) throws Exception {
        setItem(AppConstants.APPDEPL_RES_AUTH, value);
    }

    protected MultiEntryApplicationTask getTask() {
        return this.task;
    }

}

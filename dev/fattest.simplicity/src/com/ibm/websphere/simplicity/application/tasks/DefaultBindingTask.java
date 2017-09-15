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

public class DefaultBindingTask extends ApplicationTask {

    public DefaultBindingTask() {

    }

    public DefaultBindingTask(String[][] taskData) {
        super(AppConstants.DefaultBindingTask, taskData);
    }

    public DefaultBindingTask(String[] columns) {
        super(AppConstants.DefaultBindingTask, columns);
    }

    public String getDefaultDataSourceJndi() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_DDSJNDI, 1);
    }

    public void setDefaultDataSourceJndi(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_DDSJNDI, 1, value);
    }

    public String getDefaultDataSourceUser() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_DDSUSER, 1);
    }

    public void setDefaultDataSourceUser(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_DDSUSER, 1, value);
    }

    public String getDefaultDataSourcePassword() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_DDSPASS, 1);
    }

    public void setDefaultDataSourcePassword(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_DDSPASS, 1, value);
    }

    public String getConnectionFactoryJndi() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_CFJNDI, 1);
    }

    public void setConnectionFactoryJndi(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_CFJNDI, 1, value);
    }

    public String getConnectionFactoryResourceAuth() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_CFRESAUTH, 1);
    }

    public void setConnectionFactoryResourceAuth(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_CFRESAUTH, 1, value);
    }

    public String getEjbJndiPrefix() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_EJBJNDIPREFIX, 1);
    }

    public void setEjbJndiPrefix(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_EJBJNDIPREFIX, 1, value);
    }

    public String getVirtualHost() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_VHOST, 1);
    }

    public void setVirtualHost(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_VHOST, 1, value);
    }

    public boolean getForceBindings() {
        return getBoolean(AppConstants.APPDEPL_DFLTBNDG_FORCE, 1);
    }

    public void setForceBindings(Boolean value) {
        modified = true;
        setBoolean(AppConstants.APPDEPL_DFLTBNDG_FORCE, 1, value);
    }

    public String getStrategyFile() {
        return getString(AppConstants.APPDEPL_DFLTBNDG_STRATEGY, 1);
    }

    public void setStrategyFile(String value) {
        modified = true;
        setItem(AppConstants.APPDEPL_DFLTBNDG_STRATEGY, 1, value);
    }

}

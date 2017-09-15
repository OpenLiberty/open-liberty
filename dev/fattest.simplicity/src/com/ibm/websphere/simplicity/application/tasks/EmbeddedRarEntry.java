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

public class EmbeddedRarEntry extends TaskEntry {

    public EmbeddedRarEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getRarModule() {
        return getString(AppConstants.APPDEPL_RAR_MODULE);
    }

    protected void setRarModule(String value) {
        setItem(AppConstants.APPDEPL_RAR_MODULE, value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getJ2cType() {
        return getString(AppConstants.APPDEPL_J2C_TYPE);
    }

    protected void setJ2cType(String value) {
        setItem(AppConstants.APPDEPL_J2C_TYPE, value);
    }

    public String getJ2cId() {
        return getString(AppConstants.APPDEPL_J2C_ID);
    }

    protected void setJ2cId(String value) {
        setItem(AppConstants.APPDEPL_J2C_ID, value);
    }

    public String getJ2cName() {
        return getString(AppConstants.APPDEPL_J2C_NAME);
    }

    public void setJ2cName(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_J2C_NAME, value);
    }

    public String getJ2cJndi() {
        return getString(AppConstants.APPDEPL_J2C_JNDINAME);
    }

    public void setJ2cJndi(String value) {
        task.setModified();
        setItem(AppConstants.APPDEPL_J2C_JNDINAME, value);
    }

}

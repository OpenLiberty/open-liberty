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

public class MapMessageDestinationRefToEJBEntry extends TaskEntry {

    public MapMessageDestinationRefToEJBEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getModule() {
        return super.getModule();
    }

    public String getModuleVersion() throws Exception {
        return super.getModuleVersion();
    }

    public String getAppVersion() throws Exception {
        return super.getAppVersion();
    }

    public String getEjb() {
        return super.getEjb();
    }

    public String getUri() {
        return super.getUri();
    }

    public String getMessageDestinationObject() {
        return super.getItem(AppConstants.APPDEPL_MESSAGE_DESTINATION_OBJECT);
    }

    public Boolean getIsMd() {
        return super.getBoolean(AppConstants.APPDEPL_IS_MD);
    }

    public String getMessageDestinationRefName() {
        return super.getItem(AppConstants.APPDEPL_MESSAGE_DESTINATION_REF_NAME);
    }

    public String getJndi() throws Exception {
        return super.getJndi();
    }

    public String getDestJndi() {
        return super.getItem(AppConstants.APPDEPL_JNDI_DEST);
    }

    public void setModule(String value) {
        task.setModified();
        super.setModule(value);
    }

    protected void setModuleVersion(String value) throws Exception {
        super.setModuleVersion(value);
    }

    protected void setAppVersion(String value) throws Exception {
        super.setAppVersion(value);
    }

    public void setEjb(String value) {
        task.setModified();
        super.setEjb(value);
    }

    public void setUri(String value) {
        task.setModified();
        super.setUri(value);
    }

    public void setMessageDestinationObject(String value) {
        task.setModified();
        super.setItem(AppConstants.APPDEPL_MESSAGE_DESTINATION_OBJECT, value);
    }

    protected void setIsMd(Boolean value) {
        super.setBoolean(AppConstants.APPDEPL_IS_MD, value);
    }

    protected void setMessageDestinationRefName(String value) {
        super.setItem(AppConstants.APPDEPL_MESSAGE_DESTINATION_REF_NAME, value);
    }

    public void setJndi(String value) throws Exception {
        task.setModified();
        super.setJndi(value);
    }

    protected void setDestJndi(String value) {
        super.setItem(AppConstants.APPDEPL_JNDI_DEST, value);
    }

}

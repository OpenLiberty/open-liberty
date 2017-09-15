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

public class CorrectUseSystemIdentityEntry extends TaskEntry {

    public CorrectUseSystemIdentityEntry(String[] data, MultiEntryApplicationTask task) {
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

    public String getMethodSignature() {
        return getString(AppConstants.APPDEPL_METHOD_SIGNATURE, null);
    }

    protected void setMethodSignature(String value) {
        setItem(AppConstants.APPDEPL_METHOD_SIGNATURE, value);
    }

    public String getRole() {
        return super.getRole();
    }

    public void setRole(String value) {
        task.setModified();
        super.setRole(value);
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

}

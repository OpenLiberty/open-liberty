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

public class ListModulesEntry extends TaskEntry {

    public ListModulesEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    public String getModule() {
        return super.getModule();
    }

    protected void setModule(String value) {
        super.setModule(value);
    }

    public String getUri() {
        return super.getUri();
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    public String getServer() {
        return super.getServer();
    }

    protected void setServer(String value) {
        super.setServer(value);
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

}

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

public class MapEJBRefToEJBEntry extends TaskEntry {

    public MapEJBRefToEJBEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
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

    public String getClassName() {
        return super.getClassName();
    }

    protected void setClassName(String value) {
        super.setClassName(value);
    }

    public String getJndi() throws Exception {
        return super.getJndi();
    }

    public void setJndi(String value) throws Exception {
        task.setModified();
        super.setJndi(value);
    }

    public String getModuleVersion() throws Exception {
        hasAtLeast(7);
        return super.getModuleVersion();
    }

    protected void setModuleVersion(String value) throws Exception {
        hasAtLeast(7);
        super.setModuleVersion(value);
    }

}

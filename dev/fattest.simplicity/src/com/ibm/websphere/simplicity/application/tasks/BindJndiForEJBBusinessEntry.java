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

public class BindJndiForEJBBusinessEntry extends TaskEntry {

    public BindJndiForEJBBusinessEntry(String[] data, MultiEntryApplicationTask task) {
        super(data, task);
    }

    /**
     * The EJB module that contains the enterprise beans that bind to the JNDI name.
     */
    public String getEjbModule() {
        return super.getEjbModule();
    }

    /**
     * The enterprise bean that binds to the JNDI name.
     */
    public String getEjb() {
        return super.getEjb();
    }

    /**
     * The Uniform Resource Identifier (URI) specifies the location of the module
     * archive relative to the root of the application EAR.
     */
    public String getUri() {
        return super.getUri();
    }

    /**
     * The enterprise bean business interface in an EJB module.
     */
    public String getEjbBusinessInterface() {
        return super.getItem(AppConstants.APPDEPL_EJB_BUSINESS_INTERFACE);
    }

    /**
     * Specifies the JNDI name associated with the enterprise bean business interface in an EJB module.
     */
    public String getJndi() {
        return super.getItem(AppConstants.APPDEPL_EJB_BUSINESS_INTERFACE_JNDI);
    }

    protected void setEjbModule(String value) {
        super.setEjbModule(value);
    }

    protected void setEjb(String value) {
        super.setEjb(value);
    }

    protected void setUri(String value) {
        super.setUri(value);
    }

    protected void setEjbBusinessInterface(String value) {
        super.setItem(AppConstants.APPDEPL_EJB_BUSINESS_INTERFACE, value);
    }

    /**
     * Specifies the JNDI name associated with the enterprise bean business interface in an EJB module.
     */
    public void setJndi(String value) {
        task.setModified();
        super.setItem(AppConstants.APPDEPL_EJB_BUSINESS_INTERFACE_JNDI, value);
    }

}

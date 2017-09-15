/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.osgi.internal.metadata;

import com.ibm.ejs.container.EJBMethodInfoImpl;

/**
 *
 */
public class OSGiEJBMethodMetaDataImpl extends EJBMethodInfoImpl {
    String runAs = null;
    boolean useCallerPrincipal = false;
    boolean useSystemPrincipal = false;

    /**
     * @param slotSize
     */
    public OSGiEJBMethodMetaDataImpl(int slotSize) {
        super(slotSize);
    }

    @Override
    public String getRunAs() {
        return (this.runAs != null) ? this.runAs : super.getRunAs();
    }

    public void setRunAs(String runAsId) {
        this.useCallerPrincipal = false;
        this.useSystemPrincipal = false;
        this.runAs = runAsId;
    }

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is to come from the caller.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is to come from the caller.
     */
    @Override
    public boolean isUseCallerPrincipal() {
        return useCallerPrincipal;
    }

    public void setUseCallerPrincipal() {
        this.useCallerPrincipal = true;
        this.useSystemPrincipal = false;
        this.runAs = null;
    }

    /**
     * Return a boolean indicating if the identity for the execution of this
     * method is the system principle.
     * 
     * @return boolean indicating if the identity for the execution of this
     *         method is the system principle.
     */
    @Override
    public boolean isUseSystemPrincipal() {
        return useSystemPrincipal;
    }

    public void setUseSystemPrincipal() {
        this.useCallerPrincipal = false;
        this.useSystemPrincipal = true;
        this.runAs = null;
    }
}

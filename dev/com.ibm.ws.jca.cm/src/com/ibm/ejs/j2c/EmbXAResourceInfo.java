/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.j2c;

/**
 * XAResourceInfo for the lightweight server.
 * This class is only used to get the instance id for XA recovery,
 * and to transfer the CMConfigData to ConnectionManager.
 */
public class EmbXAResourceInfo extends CommonXAResourceInfoImpl {

    private static final long serialVersionUID = -2573085386297411837L;

    /**
     * Contains resource reference settings.
     */
    private final CMConfigData cmConfigData;

    /**
     * Construct EmbXAResourceInfo with the specified config data.
     * 
     * @param cmConfigData contains resource reference settings.
     */
    public EmbXAResourceInfo(CMConfigData cmConfigData) {
        this.cmConfigData = cmConfigData;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof EmbXAResourceInfo && match(cmConfigData, ((EmbXAResourceInfo) obj).getCmConfig());
    }

    /** {@inheritDoc} */
    @Override
    public String getCfName() {
        return cmConfigData.getCfKey();
    }

    /** {@inheritDoc} */
    @Override
    public CMConfigData getCmConfig() {
        return cmConfigData;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return cmConfigData == null ? 0 : cmConfigData.hashCode();
    }

    /**
     * Determine if two objects, either of which may be null, are equal.
     * 
     * @param obj1 one object.
     * @param obj2 another object.
     * 
     * @return true if the objects are equal or are both null, otherwise false.
     */
    private static final boolean match(Object obj1, Object obj2) {
        return obj1 == obj2 || (obj1 != null && obj1.equals(obj2));
    }
}
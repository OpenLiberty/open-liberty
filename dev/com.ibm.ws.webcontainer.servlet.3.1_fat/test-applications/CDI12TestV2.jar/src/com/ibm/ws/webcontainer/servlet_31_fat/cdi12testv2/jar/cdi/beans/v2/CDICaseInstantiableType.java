/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

/**
 * Enumeration for CDI instantiable types.
 */
public enum CDICaseInstantiableType {
    Servlet("Servlet"),
    Filter("Filter"),
    Listener("Listener"),
    UpgradeHandler("UpgradeHandler"),
    Unknown("Unknown");

    /**
     * Create a new CDI instantiable type enum value
     *
     * @param tag A tag to associate with the type.
     */
    private CDICaseInstantiableType(String tag) {
        this.tag = tag;
    }

    /** The tag associated with this type. */
    private final String tag;

    /**
     * Answer the tag associated with this type.
     *
     * @return The tag associated with this type.
     */
    public String getTag() {
        return tag;
    }
}

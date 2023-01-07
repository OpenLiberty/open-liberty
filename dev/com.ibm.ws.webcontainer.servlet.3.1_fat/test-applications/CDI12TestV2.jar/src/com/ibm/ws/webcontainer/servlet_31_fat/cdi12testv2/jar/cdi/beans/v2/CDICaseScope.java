/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet_31_fat.cdi12testv2.jar.cdi.beans.v2;

/**
 * CDI injection cases.
 */
public enum CDICaseScope {
    Dependent("Dependent"),
    Request("Request"),
    Conversation("Conversation"),
    Session("Session"),
    Application("Application"),
    Unknown("Unknown");

    /**
     * Create a new CDI scope case enum value
     *
     * @param tag A tag to associate with the scope case.
     */
    private CDICaseScope(String tag) {
        this.tag = tag;
    }

    /** The tag associated with this case. */
    private final String tag;

    /**
     * Answer the tag associated with this case.
     *
     * @return The tag associated with this case.
     */
    public String getTag() {
        return tag;
    }
}

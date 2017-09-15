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
package com.ibm.ws.transport.iiop.security.util;

import java.io.Serializable;

/**
 *
 */
public final class GSSExportedName implements Serializable {

    private final String name;
    private final String oid;

    public GSSExportedName(String name, String oid) {
        this.name = name;
        this.oid = oid;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

}

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
package com.ibm.wsspi.security.authorization.saf;

import com.ibm.websphere.ras.annotation.Trivial;

// !! NOTE: This enum also exists in the native code in security_saf_authorization.h
//          (saf_access_level). The two enums must be consistent with each other. !!

/**
 * SAF access level values. These values map to the ATTR= option on RACROUTE requests.
 *
 * @author IBM Corporation
 * @version 1.0
 * @ibm-spi
 */
@Trivial
public enum AccessLevel {

    /**
     * No access.
     */
    NO_ACCESS("NO_ACCESS", 0x00),

    /**
     * READ access.
     */
    READ("READ", 0x02),

    /**
     * UPDATE access.
     */
    UPDATE("UPDATE", 0x04),

    /**
     * CONTROL access.
     */
    CONTROL("CONTROL", 0x08),

    /**
     * ALTER access.
     */
    ALTER("ALTER", 0x80);

    public final String name;
    public final int value;

    AccessLevel(String name, int value) {
        this.name = name;
        this.value = value;
    }
}

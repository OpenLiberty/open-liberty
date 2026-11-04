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

// !! NOTE: This enum is also defined in native code in security_saf_authorization.h
//          (saf_log_option). The two enums must be kept consistent with each other. !!

/**
 * SAF logging values. These values are used to indicate the LOG= option on RACROUTE requests.
 *
 * @author IBM Corporation
 * @version 1.0
 * @ibm-spi
 */
@Trivial
public enum LogOption {

    /**
     * Record the event as specified by the profile that protects
     * the resource or via options such as SETROPTS.
     */
    ASIS("ASIS", 1),

    /**
     * If the authorization fails, the attempt is not recorded.
     * If authorization is successful, the event is recorded as in ASIS.
     */
    NOFAIL("NOFAIL", 2),

    /**
     * The authorization event is not recorded and messages are
     * suppressed regardless of whether or not MSGSUPP=NO is specified.
     */
    NONE("NONE", 3),

    /**
     * The attempt is not recorded and no statistics are updated.
     */
    NOSTAT("NOSTAT", 4);

    public final String name;
    public final int value;

    LogOption(String name, int value) {
        this.name = name;
        this.value = value;
    }
}

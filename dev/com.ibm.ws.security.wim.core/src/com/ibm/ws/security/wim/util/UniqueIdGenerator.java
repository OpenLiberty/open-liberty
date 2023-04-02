/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
package com.ibm.ws.security.wim.util;

import java.util.UUID;

/**
 * This class wraps the JDK 1.5 UUID class to generate UUID. JDK's UUID class
 * can directly be used. This class is created to maintain backward compatibility
 * with the code which use JDK 1.4.x.
 */
public class UniqueIdGenerator {

    /**
     * Generates a UniqueId string
     *
     * @return the generated UniqueId
     */
    public static synchronized String newUniqueId() {
        return UUID.randomUUID().toString();
    }
}

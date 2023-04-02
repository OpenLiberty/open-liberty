/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
package com.ibm.ws.security.wim;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Some helper methods for working with {@link AccessController} operations.
 */
public class AccessControllerHelper {

    /**
     * Convenience method to get a system property using
     * {@link AccessController#doPrivileged(PrivilegedAction)}.
     *
     * @param property The property to retrieve.
     * @return The value returned from {@link System#getProperty(String)}.
     */
    public static String getSystemProperty(final String property) {
        return AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty(property);
            }
        });
    }
}

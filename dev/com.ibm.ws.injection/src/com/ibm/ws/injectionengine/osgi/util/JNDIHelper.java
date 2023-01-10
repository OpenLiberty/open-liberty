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
package com.ibm.ws.injectionengine.osgi.util;

/**
 *
 */
public class JNDIHelper {
    /**
     * Return true if a JNDI name has a scheme.
     */
    public static boolean hasJNDIScheme(String jndiName) {
        int colonIndex = jndiName.indexOf(':');
        int slashIndex = jndiName.indexOf('/');
        return colonIndex != -1 && (slashIndex == -1 || colonIndex < slashIndex);
    }
}

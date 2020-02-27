/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.internal;

/**
 * Utility class to assist with HTTP related operations
 */
public class Utils {

    /**
     * Convert the String array to a String.
     * 
     * @param value
     * @return
     */
    public static String toString(String[] value) {
        String result = null;
        if (value != null && value.length > 0) {
            StringBuffer buf = null;
            for (int i = 0; i < value.length; i++) {
                if (buf == null) {
                    buf = new StringBuffer("[ ");
                } else {
                    buf.append(", ");
                }
                buf.append(value[i]);
            }
            buf.append(" ]");
            result = buf.toString();
        }
        return result;
    }

}

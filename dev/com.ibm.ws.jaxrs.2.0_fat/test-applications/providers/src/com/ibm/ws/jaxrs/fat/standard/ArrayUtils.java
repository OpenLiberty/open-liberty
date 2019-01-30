/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.standard;

public class ArrayUtils {

    /**
     * copyOf performs the very same function that JDK6 java.util.Arrays.copyOf
     * performs. We need it here to support JDK5
     *
     * @param buffer
     * @param size
     * @return
     */
    public static byte[] copyOf(byte[] buffer, int size) {
        byte[] copy = new byte[size];
        System.arraycopy(buffer, 0, copy, 0, Math.min(buffer.length, size));
        return copy;
    }

    /**
     * copyOf performs the very same function that JDK6 java.util.Arrays.copyOf
     * performs. We need it here to support JDK5
     *
     * @param buffer
     * @param size
     * @return
     */
    public static char[] copyOf(char[] buffer, int size) {
        char[] copy = new char[size];
        System.arraycopy(buffer, 0, copy, 0, Math.min(buffer.length, size));
        return copy;
    }

}

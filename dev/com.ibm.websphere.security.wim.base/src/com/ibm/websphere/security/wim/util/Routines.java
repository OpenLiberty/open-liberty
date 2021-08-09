/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.security.wim.util;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Routines class is used to specify functions which can be used for variety
 * of reasons, mostly consists of helper functions.
 *
 */
@Trivial
public class Routines {
    private static String newline = System.getProperty("line.separator");

    public static Object[] arrayCopy(Object[] inArray) {
        Object[] outArray = null;
        if (inArray != null) {
            outArray = new Object[inArray.length];
            System.arraycopy(inArray, 0, outArray, 0, inArray.length);
        }
        return outArray;
    }

}

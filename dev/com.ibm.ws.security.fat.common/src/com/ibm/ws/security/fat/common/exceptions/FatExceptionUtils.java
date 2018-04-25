/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.exceptions;

import java.util.List;

public class FatExceptionUtils {

    protected static Class<?> thisClass = FatExceptionUtils.class;

    public static Exception buildCumulativeException(List<Exception> exceptions) {
        if (exceptions == null) {
            return null;
        }
        Exception cumulativeException = null;
        int exceptionNum = 0;
        for (Exception e : exceptions) {
            if (e == null) {
                continue;
            }
            String prevExceptionMsg = (cumulativeException == null) ? "" : (cumulativeException.getMessage() + "\n<br/>");
            cumulativeException = new Exception(prevExceptionMsg + "[Exception #" + (++exceptionNum) + "]: " + e.getMessage());
        }
        return cumulativeException;
    }

}

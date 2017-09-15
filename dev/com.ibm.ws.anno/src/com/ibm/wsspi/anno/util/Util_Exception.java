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

package com.ibm.wsspi.anno.util;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class Util_Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public static final String CLASS_NAME = Util_Exception.class.getName();

    //

    public Util_Exception(String message) {
        super(message);
    }

    public Util_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    //

    public static Util_Exception wrap(TraceComponent tc, String callingClassName,
                                      String callingMethodName, String message, Throwable th) {

        Util_Exception wrappedException = new Util_Exception(message, th);

        if (tc.isEventEnabled()) {

            Tr.event(tc, MessageFormat.format("[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                                              new Object[] { callingClassName, callingMethodName,
                                                            th.getClass().getName(),
                                                            wrappedException.getClass().getName() }));

            Tr.event(tc, th.getMessage(), th);
            Tr.event(tc, message, wrappedException);
        }

        return wrappedException;
    }
}

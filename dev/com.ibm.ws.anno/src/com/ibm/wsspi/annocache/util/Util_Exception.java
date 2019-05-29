/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.annocache.util;

import java.util.logging.Logger;
import java.util.logging.Level;

public class Util_Exception extends com.ibm.wsspi.anno.util.Util_Exception {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("hiding")
	public static final String CLASS_NAME = "Util_Exception";

    //

    public Util_Exception(String message) {
        super(message);
    }

    public Util_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    //

    public static Util_Exception wrap(Logger logger, String callingClassName,
                                      String callingMethodName, String message, Throwable th) {

        String methodName = "wrap";

        Util_Exception wrappedException = new Util_Exception(message, th);

        if ( logger.isLoggable(Level.FINER) ) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                    new Object[] { callingClassName, callingMethodName,
                                   th.getClass().getName(),
                                   wrappedException.getClass().getName() });
        }

        return wrappedException;
    }
}

/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corporation 2011, 2018
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */

package com.ibm.wsspi.anno.info;

import java.util.logging.Level;
import java.util.logging.Logger;

public class InfoStoreException extends Exception {
    private static final long serialVersionUID = 1L;

    public static final String CLASS_NAME = "InfoStoreException";

    //

    public InfoStoreException(String message) {
        super(message);
    }

    public InfoStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    //

    public static InfoStoreException wrap(Logger logger,
                                          String callingClassName,
                                          String callingMethodName,
                                          String message, Throwable th) {
        String methodName = "wrap";

        InfoStoreException wrappedException = new InfoStoreException(message, th);

        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    "[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                    new Object[] { callingClassName, callingMethodName,
                                   th.getClass().getName(),
                                   wrappedException.getClass().getName() });
        }

        return wrappedException;
    }
}

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

package com.ibm.wsspi.anno.targets;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AnnotationTargets_Exception extends Exception {
    private static final long serialVersionUID = 1L;

    public static final String CLASS_NAME = "AnnotationTargets_Exception";

    //

    public AnnotationTargets_Exception(String message) {
        super(message);
    }

    public AnnotationTargets_Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public static AnnotationTargets_Exception wrap(Logger logger,
                                                   String callingClassName,
                                                   String callingMethodName,
                                                   String message, Throwable th) {
        AnnotationTargets_Exception wrappedException = new AnnotationTargets_Exception(message, th);

        String methodName = "wrap";
        
        if (logger.isLoggable(Level.FINER)) {
            logger.logp(Level.FINER, CLASS_NAME, methodName,
                    " [ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                    new Object[] { callingClassName, 
                                   callingMethodName,
                                   th.getClass().getName(),
                                   wrappedException.getClass().getName() });
        }

        return wrappedException;
    }
}

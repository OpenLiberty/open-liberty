/*******************************************************************************
 * Copyright (c) 2017, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.annocache.targets;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AnnotationTargets_Exception extends com.ibm.wsspi.anno.targets.AnnotationTargets_Exception {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("hiding")
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

/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.wsspi.anno.info;

import java.text.MessageFormat;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class InfoStoreException extends Exception {
    private static final long serialVersionUID = 1L;

    public static final String CLASS_NAME = InfoStoreException.class.getName();

    //

    public InfoStoreException(String message) {
        super(message);
    }

    public InfoStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    //

    public static InfoStoreException wrap(TraceComponent logger,
                                          String callingClassName,
                                          String callingMethodName,
                                          String message, Throwable th) {

        InfoStoreException wrappedException = new InfoStoreException(message, th);

        if (logger.isDebugEnabled()) {
            Tr.debug(logger, MessageFormat.format("[ {0} ] [ {1} ] Wrap [ {2} ] as [ {3} ]",
                                                  callingClassName, callingMethodName,
                                                  th.getClass().getName(),
                                                  wrappedException.getClass().getName()));

            Tr.debug(logger, th.getMessage(), th);
            Tr.debug(logger, message, wrappedException);
        }

        return wrappedException;
    }
}

/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.instrument.serialfilter.validators;

import java.io.InvalidClassException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.kernel.instrument.serialfilter.util.MessageUtil;

class RejectingClassValidator extends ClassValidator {
    private final Logger log;

    RejectingClassValidator() {
        log = Logger.getLogger(RejectingClassValidator.class.getName());
    }

    @Override
    protected Class<?> apply(Class<?> cls) throws InvalidClassException {
        if (cls == null)
            return null;
        String message = MessageUtil.format("SF_ERROR_REJECT", cls.getName());
        if (log.isLoggable(Level.SEVERE)) log.severe(String.format(message));
        throw new InvalidClassException(message);
    }

    private static ClassLoader getLoader(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return cls.getClassLoader();
            }
        });
    }
}

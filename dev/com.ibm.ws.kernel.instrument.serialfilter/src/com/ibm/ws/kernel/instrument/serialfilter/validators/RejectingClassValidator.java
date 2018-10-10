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

class RejectingClassValidator extends ClassValidator {
    private final Logger log;

    RejectingClassValidator() {
        log = Logger.getLogger(RejectingClassValidator.class.getName());
    }

    @Override
    protected Class<?> apply(Class<?> cls) throws InvalidClassException {
        if (cls == null)
            return null;
        // TODO NLS-ify this message
        if (log.isLoggable(Level.INFO))
            log.info(String.format("The following class was rejected. Class name : %s", cls.getName()));
        throw new InvalidClassException(cls.getName(), "Deserialization of Java objects is disallowed within this context.");
    }

    private static ClassLoader getLoader(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return cls.getClassLoader();
            }
        });
    }
}

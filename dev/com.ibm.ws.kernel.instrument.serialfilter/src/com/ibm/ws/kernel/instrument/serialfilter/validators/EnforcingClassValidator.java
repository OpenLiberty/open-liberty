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

import com.ibm.ws.kernel.instrument.serialfilter.config.Config;
import com.ibm.ws.kernel.instrument.serialfilter.util.MessageUtil;

import java.io.InvalidClassException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

class EnforcingClassValidator extends ClassValidator {
    private final Logger log;
    private final Class<?>[] skipOnce = {null};
    private final Config config;

    EnforcingClassValidator(Config config) {
        this.config = config;
        log = Logger.getLogger(EnforcingClassValidator.class.getName());
    }

    @Override
    protected Class<?> apply(Class<?> cls) throws InvalidClassException {
        if (cls == null) return null;
        if (cls.isArray()) return cls;
        if (log.isLoggable(Level.FINEST)) log.finest(String.format("log Validating class - loaded by [%s] with name [%s]%n", getLoader(cls), cls.getName()));
        if (config.allows(cls, skipOnce)) return cls;
        throw new InvalidClassException(MessageUtil.format("SF_ERROR_NOT_ON_WHITELIST", cls.getName()));
    }

    private static ClassLoader getLoader(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return cls.getClassLoader();
            }
        });
    }
}

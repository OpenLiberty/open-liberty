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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static java.util.logging.Level.INFO;
import static java.util.logging.Level.FINEST;

class DiscoveringClassValidator extends ClassValidator {
    private final Logger log;
    private static final AtomicInteger NEXT_INT = new AtomicInteger(1);
    private final Class<?>[] skipOnce = {null};
    private final int id = NEXT_INT.getAndIncrement();
    private final Config config;

    DiscoveringClassValidator(Config config) {
        this.config = config;
        log = Logger.getLogger(DiscoveringClassValidator.class.getName());
    }

    @Override
    public Class<?> apply(Class<?> cls) {
        if (cls == null) return null;
        if (cls.isArray()) return cls;
        if (log.isLoggable(FINEST)) log.finest(String.format("Discovering. Class name : %s ClassLoader name : %s", cls.getName(), getLoader(cls)));
        boolean whitelisted = config.allows(cls, skipOnce, false);
        if (whitelisted)
            return cls;
        if (log.isLoggable(FINEST)) log.finest(String.format("The class is not on the whitelist. Class name : %s ClassLoader name : %s", cls.getName(), getLoader(cls)));
        if (log.isLoggable(INFO)) log.info(MessageUtil.format("SF_INFO_NOT_ON_WHITELIST", cls.getName()));
        return cls;
    }

    private static ClassLoader getLoader(final Class<?> cls) {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return cls.getClassLoader();
            }
        });
    }

    @Override
    public void reset() {skipOnce[0] = null;}
}

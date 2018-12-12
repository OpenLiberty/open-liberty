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

import java.util.logging.Level;
import java.util.logging.Logger;

/** Perform no validation at all. */
class InactiveClassValidator extends ClassValidator {
    private final Logger log;
    public static final InactiveClassValidator INSTANCE = new InactiveClassValidator();

    private InactiveClassValidator(){
        log = Logger.getLogger(InactiveClassValidator.class.getName());
    }

    @Override
    protected Class<?> apply(Class<?> cls) {
        if (log.isLoggable(Level.FINEST))
            log.finest("Not validating " + cls.getName());
        return cls;
    }

}

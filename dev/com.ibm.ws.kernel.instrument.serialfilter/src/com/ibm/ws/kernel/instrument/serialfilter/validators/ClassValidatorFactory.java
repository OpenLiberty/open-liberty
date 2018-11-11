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
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigHolder;
import com.ibm.ws.kernel.instrument.serialfilter.config.ValidationMode;

import java.io.ObjectInputStream;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.FINEST;

final class ClassValidatorFactory extends ConfigHolder {
    private final Logger log;
    private final Config config;

    ClassValidatorFactory(Config config) {
        super(config);
        this.config = config;
        log = Logger.getLogger(ClassValidatorFactory.class.getName());
    }

    /**
     * This method should be called by ObjectInputStream's constructors.
     * It will examine the call stack and compare it to the provided
     * configuration to determine the correct type of validation needed
     * by the ObjectInputStream under construction.
     *
     * @return an appropriate validator object, disguised as a Map
     */
    public Map<Class<?>, Class<?>> apply(ObjectInputStream caller) throws Exception {
        return createValidator(caller);
    }

    private ClassValidator createValidator(ObjectInputStream caller) {
        if (log.isLoggable(FINEST)) {
            log.finest("Creating validator for " + caller.getClass().getName());
        }

        ValidationMode mode = config.getModeForStack(caller.getClass());

        ClassValidator cv = getValidator(mode);
        if (log.isLoggable(FINEST)) {
            log.finest("Validator : " + cv);
        }
        return cv;
    }

    private ClassValidator getValidator(ValidationMode mode) {
        switch(mode) {
            case INACTIVE:
                return InactiveClassValidator.INSTANCE;
            case DISCOVER:
                return new DiscoveringClassValidator(config);
            case ENFORCE:
                return new EnforcingClassValidator(config);
            case REJECT:
                return new RejectingClassValidator();
            default:
                throw new IllegalArgumentException("Unexpected mode: " + mode);
        }
    }

}


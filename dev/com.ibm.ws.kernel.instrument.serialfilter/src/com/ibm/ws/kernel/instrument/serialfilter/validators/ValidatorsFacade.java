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
import com.ibm.ws.kernel.instrument.serialfilter.config.ConfigurableFunctor;
import com.ibm.ws.kernel.instrument.serialfilter.config.SimpleConfig;

import java.io.ObjectInputStream;
import java.util.Map;

public abstract class ValidatorsFacade {
    private ValidatorsFacade(){}

    public static ConfigurableFunctor<SimpleConfig, ObjectInputStream, Map<Class<?>, Class<?>>> createFactory(Config config) {
        return new ClassValidatorFactory(config);
    }
}

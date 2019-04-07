/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.interfaces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;

/**
 * Constants for Config 1.4
 */
public class Config14Constants {

    //the tokens that mark the start and end of variables to be evaluated
    public static final String EVAL_START_TOKEN = "${";
    public static final String EVAL_END_TOKEN = "}";

    public static final String DEFAULT_STRING_SOURCE_NAME = "_DEFAULT_STRING_";
    public static final String DEFAULT_VALUE_SOURCE_NAME = "_DEFAULT_VALUE_";

    public static final boolean ACCESSOR_EVALUATE_VARIABLES_DEFAULT = getEvaluateVariable();
    public static final boolean CONFIG_EVALUATE_VARIABLES_DEFAULT = false;

    @Trivial
    private static final boolean getEvaluateVariable() {
        boolean evaluateVariables;
        try {
            evaluateVariables = (boolean) ConfigProperty.class.getMethod("evaluateVariables").getDefaultValue();
        } catch (IllegalArgumentException | NoSuchMethodException | SecurityException e) {
            throw new ConfigException(e);
        }
        return evaluateVariables;
    }

}

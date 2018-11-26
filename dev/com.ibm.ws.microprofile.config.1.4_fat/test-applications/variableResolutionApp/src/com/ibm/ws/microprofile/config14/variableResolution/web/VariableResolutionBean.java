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
package com.ibm.ws.microprofile.config14.variableResolution.web;

import static org.junit.Assert.assertEquals;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;

@Dependent
public class VariableResolutionBean {

    @Inject
    Config config;

    public void variableResolutionTest() {
        ConfigAccessor<String> accessor = config.access("layer1");
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.one.two.end", value);
    }

    public void disabledVariableResolutionTest() {
        ConfigAccessor<String> accessor = config.access("layer1");
        accessor.evaluateVariables(false);
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.${layer2}.end", value);
    }

}

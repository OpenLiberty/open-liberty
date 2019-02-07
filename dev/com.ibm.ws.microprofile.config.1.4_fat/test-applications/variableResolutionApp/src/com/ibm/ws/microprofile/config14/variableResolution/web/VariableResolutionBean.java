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
import org.eclipse.microprofile.config.ConfigAccessorBuilder;

@Dependent
public class VariableResolutionBean {

    @Inject
    Config config;

    public void variableResolutionTest() {
        ConfigAccessorBuilder<String> builder = config.access("layer1", String.class);
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.one.two.end", value);
    }

    public void disabledVariableResolutionTest() {
        ConfigAccessorBuilder<String> builder = config.access("layer1", String.class);
        builder.evaluateVariables(false);
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.${layer2}.end", value);
    }

}

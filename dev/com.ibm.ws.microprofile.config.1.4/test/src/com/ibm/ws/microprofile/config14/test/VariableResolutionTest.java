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
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

/**
 *
 */
public class VariableResolutionTest extends AbstractConfigTest {

    @Test
    public void variableResolutionTest() {
        Config config = ConfigProviderResolver.instance().getConfig();
        ConfigAccessor<String> accessor = config.access("layer1");
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.one.two.end", value);
    }

    @Test
    public void disabledVariableResolutionTest() {
        Config config = ConfigProviderResolver.instance().getConfig();
        ConfigAccessor<String> accessor = config.access("layer1");
        accessor.evaluateVariables(false);
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "start.${layer2}.end", value);
    }

}

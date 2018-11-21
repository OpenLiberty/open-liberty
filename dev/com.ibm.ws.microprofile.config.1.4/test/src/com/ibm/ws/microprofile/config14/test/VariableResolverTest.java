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
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config14.impl.Config14Impl;

/**
 *
 */
public class VariableResolverTest extends AbstractConfigTest {

//    @Test
//    public void variableResolutionTest() {
//        Config config = ConfigProviderResolver.instance().getConfig();
//        ConfigAccessor<String> accessor = config.access("layer1");
//        String value = accessor.getValue();
//        assertEquals("Value not correctly resolved", "start.one.two.end", value);
//    }
//
//    @Test
//    public void disabledVariableResolutionTest() {
//        Config config = ConfigProviderResolver.instance().getConfig();
//        ConfigAccessor<String> accessor = config.access("layer1");
//        accessor.evaluateVariables(false);
//        String value = accessor.getValue();
//        assertEquals("Value not correctly resolved", "start.${layer2}.end", value);
//    }

    @Test
    public void testResolver() {
        String raw = "${layer1}";
        Config config = ConfigProviderResolver.instance().getConfig();
        String resolved = Config14Impl.resolve3(config, raw);
        assertEquals("Value not correctly resolved", "{start.one.two_dollar_brace.end}", resolved);
    }

    // {start.${layer5}.${layer${layer4}}.end}
    // to be resolved: layer5 -> one
    // to be resolved: layer${layer4}
    // to be resolved: layer4 -> two${
    // {start.one.two_dollar_brace.end}

}

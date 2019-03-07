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
package com.ibm.ws.microprofile.config14.test;

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.microprofile.config14.impl.PropertyResolverUtil;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public class VariableResolverTest extends AbstractConfigTest {

    @BeforeClass
    public static void setup() {
        System.setProperty("layer1", "{start.${layer5}.${layer${layer4}}.end}");
        System.setProperty("layertwo", "${layer2}");
        System.setProperty("layer2", "${layer3}.${layer4}");
        System.setProperty("layertwo${", "two_dollar_brace");
        System.setProperty("layer3", "${layer5}");
        System.setProperty("layer4", "two${");
        System.setProperty("layer5", "one");
        System.getProperties().put("", "nothing");
    }

    @Test
    public void variableResolutionTest() {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("layer1", String.class);
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "{start.one.two_dollar_brace.end}", value);
    }

    @Test
    public void disabledVariableResolutionTest() {
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        ConfigAccessorBuilder<String> builder = config.access("layer1", String.class);
        builder.evaluateVariables(false);
        ConfigAccessor<String> accessor = builder.build();
        String value = accessor.getValue();
        assertEquals("Value not correctly resolved", "{start.${layer5}.${layer${layer4}}.end}", value);
    }

    @Test
    public void testResolver() {
        String raw = "${layer1}";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "{start.one.two_dollar_brace.end}", resolved);
    }

    @Test
    public void testUnbalancedResolver1() {
        String raw = "${${layer1}";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "${{start.one.two_dollar_brace.end}", resolved);
    }

    @Test
    public void testUnbalancedResolver2() {
        String raw = "a${b${layer1}";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "a${b{start.one.two_dollar_brace.end}", resolved);
    }

    @Test
    public void testUnbalancedResolver3() {
        String raw = "${layer1}${";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "{start.one.two_dollar_brace.end}${", resolved);
    }

    @Test
    public void testUnbalancedResolver4() {
        String raw = "${layer1}${a";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "{start.one.two_dollar_brace.end}${a", resolved);
    }

    @Test
    public void testResolverEmptyPropertyName() {
        String raw = "${}";
        WebSphereConfig14 config = (WebSphereConfig14) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();
        String resolved = PropertyResolverUtil.resolve(config, raw);
        assertEquals("Value not correctly resolved", "nothing", resolved);
    }

}

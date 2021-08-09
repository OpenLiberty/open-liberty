/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.test.converters;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Type;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

public class ConversionUtil extends AbstractConfigTest {

    public static <T> void assertConversion(String key, String rawString, T expected, Class<T> type) {
        System.setProperty(key, rawString);
        Config config = ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();

        T value = config.getValue(key, type);
        assertEquals(expected, value);
    }

    public static void assertTypedConversion(String key, String rawString, Object expected, Type type) {
        System.setProperty(key, rawString);
        WebSphereConfig config = (WebSphereConfig) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();

        Object value = config.getValue(key, type);
        assertEquals(expected, value);
    }
}
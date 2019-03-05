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
package com.ibm.ws.microprofile.config12.test.converters;

import static org.junit.Assert.assertEquals;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.junit.Test;

import com.ibm.ws.microprofile.config.interfaces.WebSphereConfig;

/**
 *
 */
public class ClassConverterTest extends AbstractConfigTest {

    @Test
    public void testClassConverter() {

        WebSphereConfig config = (WebSphereConfig) ConfigProviderResolver.instance().getBuilder().addDefaultSources().build();

        String value = "com.ibm.ws.microprofile.config12.test.converters.MyObject";
        System.out.println("String :" + value);
        Class<?> converted = config.convertValue(value, Class.class);
        assertEquals(MyObject.class, converted);
    }

}

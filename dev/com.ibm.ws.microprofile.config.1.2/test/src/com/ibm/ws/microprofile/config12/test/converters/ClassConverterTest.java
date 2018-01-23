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

import org.junit.Test;

import com.ibm.ws.microprofile.config12.converters.Config12DefaultConverters;

/**
 *
 */
public class ClassConverterTest {

    @Test
    public void testClassConverter() {
        String value = "com.ibm.ws.microprofile.config12.test.converters.MyObject";
        System.out.println("String :" + value);
        Class<?> converted = (Class<?>) Config12DefaultConverters.getDefaultConverters().getConverter(Class.class).convert(value);
        assertEquals(MyObject.class, converted);
    }

}

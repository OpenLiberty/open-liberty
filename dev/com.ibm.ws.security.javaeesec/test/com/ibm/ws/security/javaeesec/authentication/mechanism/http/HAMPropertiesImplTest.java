/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.authentication.mechanism.http;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

/**
 *
 */
public class HAMPropertiesImplTest {

    @Test
    public void testGetProperties() throws Exception {
        Properties props = new Properties();
        HAMProperties hamp = new HAMPropertiesImpl(String.class, props);
        assertEquals("It should return the same properties object", props, hamp.getProperties());
    }

    @Test
    public void testGetImplementationClass() throws Exception {
        Properties props = new Properties();
        Class c = String.class;
        HAMProperties hamp = new HAMPropertiesImpl(c, props);
        assertEquals("It should return the same implementation class name", c, hamp.getImplementationClass());
    }
}

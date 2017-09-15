/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal;

import static com.ibm.ws.classloading.internal.TestUtil.buildMockContainer;
import static com.ibm.ws.classloading.internal.TestUtil.getServletJarURL;
import static com.ibm.ws.classloading.internal.TestUtil.getTestClassesURL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.net.URL;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.wsspi.adaptable.module.Container;

public class MockContainerTest {
    @Rule
    public final SharedOutputManager outputManager = SharedOutputManager.getInstance();

    private static final String KNOWN_RESOURCE = MockContainerTest.class.getName().replace('.', '/') + ".class";

    @Test
    public void testContainer() throws Exception {
        URL testClasses = getTestClassesURL();
        Container c = buildMockContainer("testClasses", testClasses);
        assertNull("MonkeyFish should be null", c.getEntry("MonkeyFish"));

        assertNotNull(KNOWN_RESOURCE + " entry should not be null ", c.getEntry(KNOWN_RESOURCE));
        assertNotNull(KNOWN_RESOURCE + " should have resource data", c.getEntry(KNOWN_RESOURCE).getResource());

        Container c2 = buildMockContainer("servletClasses", getServletJarURL());
        assertNull("MonkeyFish should still be null", c2.getEntry("MonkeyFish"));

        assertNull("java.lang.Object.class should not be available", c.getEntry("java/lang/Object.class"));
    }
}

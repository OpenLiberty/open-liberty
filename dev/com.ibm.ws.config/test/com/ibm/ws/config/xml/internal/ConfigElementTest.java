/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

public class ConfigElementTest {

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();

        // Restore back to old kernel and let next test case set to new kernel
        // as needed
        SharedLocationManager.resetWsLocationAdmin();
    }

    @Before
    public void setUp() throws Exception {}

    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation, this keeps
        // things sane
        outputMgr.resetStreams();
    }

    @Test
    public void testCopyConstructor() throws Exception {
        String testValue = "my simple value";

        ConfigElement one = new SimpleElement("one");
        one.addCollectionAttribute("values", "1");
        one.addCollectionAttribute("values", "2");
        one.addAttribute("value", testValue);

        ConfigElement oneCopy = new SimpleElement(one);
        oneCopy.addCollectionAttribute("values", "3");

        assertEquals(Arrays.asList("1", "2"), one.getAttribute("values"));
        assertEquals(Arrays.asList("1", "2", "3"), oneCopy.getAttribute("values"));

        assertEquals(testValue, one.getAttribute("value"));
        assertEquals(testValue, oneCopy.getAttribute("value"));
    }

    @Test
    public void testCaseSensitivity() {
        ConfigElement one = new SimpleElement("one");

        one.addAttribute("value", "1");
        one.addAttribute("Value", "2");
        assertEquals("Not case-insensitive", "2", one.getAttribute("value"));

        one.addCollectionAttribute("values", "1");
        one.addCollectionAttribute("Values", "2");
        assertEquals("Not case-insensitive", Arrays.asList("1", "2"), one.getAttribute("VALues"));
    }

    @Test
    public void testDisplayId() {
        SimpleElement childElement1 = new SimpleElement("child");
        childElement1.addCollectionAttribute("value", "A");
        childElement1.setId("childId");
        SimpleElement childElement2 = new SimpleElement("child");
        childElement2.addCollectionAttribute("value", "B");
        childElement2.setId("child2Id");

        SimpleElement one = new SimpleElement("one");
        one.setId("parentId");
        one.addChildConfigElement("child", childElement2);
        one.addCollectionAttribute("values", "1");
        one.addCollectionAttribute("values", "2");
        one.addChildConfigElement("child", childElement1);

        assertEquals("one[parentId]/child[childId]", childElement1.getDisplayId());
        assertEquals("one[parentId]/child[child2Id]", childElement2.getDisplayId());
        assertEquals("one[parentId]", one.getDisplayId());
    }
}

/*******************************************************************************
 * Copyright (c) 2010, 2014 IBM Corporation and others.
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

import com.ibm.ws.config.xml.internal.metatype.MetaTypeHelper;

import test.common.SharedLocationManager;
import test.common.SharedOutputManager;

public class MetaTypeHelperTest {

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
    public void testParseValue() throws Exception {
        assertEquals(Arrays.asList(""), MetaTypeHelper.parseValue(""));

        assertEquals(Arrays.asList("simple text"), MetaTypeHelper.parseValue("simple text"));

        assertEquals(Arrays.asList("simple text"), MetaTypeHelper.parseValue("  simple text   "));

        assertEquals(Arrays.asList("simple    text"), MetaTypeHelper.parseValue("  simple    text   "));

        assertEquals(Arrays.asList("value a", "value b"), MetaTypeHelper.parseValue("value a, value b"));

        assertEquals(Arrays.asList("value a", "value b"), MetaTypeHelper.parseValue("  value a  ,  value b  "));

        assertEquals(Arrays.asList(" value a ", "value  b  "), MetaTypeHelper.parseValue("\\ value a\\ , value  b \\ "));

        assertEquals(Arrays.asList("", " value a , value  b  "), MetaTypeHelper.parseValue(",\\ value a\\ \\, value  b \\ "));
    }

    @Test
    public void testEscapeValue() throws Exception {
        assertEquals("", MetaTypeHelper.escapeValue(""));
        assertEquals("simple text", MetaTypeHelper.escapeValue("simple text"));
        assertEquals("\\ \\ simple text\\ \\ \\ ", MetaTypeHelper.escapeValue("  simple text   "));
        assertEquals("\\ \\ simple    text\\ \\ \\ ", MetaTypeHelper.escapeValue("  simple    text   "));
        assertEquals("value a\\, value b", MetaTypeHelper.escapeValue("value a, value b"));
        assertEquals("\\ \\ value a  \\,  value b\\ \\ ", MetaTypeHelper.escapeValue("  value a  ,  value b  "));
        assertEquals("\\\\ value a\\\\ \\, value  b \\\\\\ ", MetaTypeHelper.escapeValue("\\ value a\\ , value  b \\ "));
        assertEquals("\\,\\\\ value a\\\\ \\\\\\, value  b \\\\\\ ", MetaTypeHelper.escapeValue(",\\ value a\\ \\, value  b \\ "));

        for (String s : new String[] {
                                      "",
                                      "simple text",
                                      "  simple text   ",
                                      "  simple    text   ",
                                      "value a, value b",
                                      "  value a  ,  value b  ",
                                      "\\ value a\\ , value  b \\ ",
                                      ",\\ value a\\ \\, value  b \\ ",
        }) {
            assertEquals(Arrays.asList(s), MetaTypeHelper.parseValue(MetaTypeHelper.escapeValue(s)));
        }
    }
}

/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.logging.internal.TraceSpecification.TraceElement;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

/**
 * Tests for the {@link TraceSpecification} class
 */
public class TraceSpecificationTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance().logTo(TestConstants.BUILD_TMP);
    static File testLogDir;

    @Rule
    public TestRule outputRule = outputMgr;

    /**
     * This makes sure that if a single class has a setting put on it then *=info=enabled is still set for everything else
     */
    @Test
    public void testInfoEnabledForAll() {
        TraceSpecification testObject = new TraceSpecification("com.ibm.ws.SomeClass=fine=enabled", null, false);
        List<TraceElement> specs = testObject.getSpecs();
        assertEquals("There was only one spec created", 2, specs.size());

        // Find the "*" setting
        TraceElement starSetting = null;
        for (TraceElement spec : specs) {
            if ("*".equals(spec.groupName)) {
                starSetting = spec;
                break;
            }
        }
        assertNotNull("No spec was found for *", starSetting);

        // Now make sure it's set to audit
        assertEquals("The level sequence number for the * trace setting should be 6", 6, starSetting.fineLevel);
        assertTrue("The action for the * trace setting should be true to enable trace", starSetting.action);
    }

    /**
     * This makes sure that if the user supplies a setting for * then we don't override it with audit
     */
    @Test
    public void testUserOverrideForAll() {
        TraceSpecification testObject = new TraceSpecification("*=info=enabled", null, false);
        List<TraceElement> specs = testObject.getSpecs();
        assertEquals("The wrong number of specs was created, only expected 1 as the user defined a * setting", 1, specs.size());

        // Now make sure it's set to info
        TraceElement starSetting = specs.get(0);
        assertEquals("No spec was found for *", "*", starSetting.groupName);
        assertEquals("The level sequence number for the * trace setting should be 6", 6, starSetting.fineLevel);
        assertTrue("The action for the * trace setting should be true to enable trace", starSetting.action);
    }

    /**
     * This makes sure that if the user supplies a setting containing
     * extraneous whitespace, that we still parse the value just fine
     */
    @Test
    public void testWhiteSpace() {
        TraceSpecification testObject = new TraceSpecification("*=info=enabled: whitespace= all\n=\t disabled", null, false);
        List<TraceElement> specs = testObject.getSpecs();
        assertEquals("Two trace specifications should be created", 2, specs.size());

        // Now make sure we have two: *=info=enabled and whitespace=all=disabled
        TraceElement starSetting = specs.get(0);
        assertEquals("No spec was found for *", "*", starSetting.groupName);
        assertEquals("The level sequence number for the * trace setting should be 6", 6, starSetting.fineLevel);
        assertTrue("The action for the * trace setting should be true to enable trace", starSetting.action);
        assertEquals("The summary string equal the default", "*=info=enabled", starSetting.fullString);

        TraceElement whitespaceSetting = specs.get(1);
        assertEquals("No spec was found for whitespace", "whitespace", whitespaceSetting.groupName);
        assertEquals("The level sequence number for the whitespace trace setting", 0, whitespaceSetting.fineLevel);
        assertFalse("The action for the whitespace trace setting should be true to disable trace", whitespaceSetting.action);
        assertEquals("The full string should have whitespace removed", "whitespace=all=disabled", whitespaceSetting.fullString);
    }

    @Test
    public void testEquals() {
        TraceSpecification testSpec = new TraceSpecification("*=info=enabled: whitespace= all\n=\t disabled", null, false);
        TraceSpecification testSpec2 = new TraceSpecification("*=info:whitespace=all=disabled", null, false);
        assertEquals("Test specifications should compare equal", testSpec, testSpec2);
    }

    @Test
    public void testEqualsSameStringDifferentSuppressSensitiveTraceValue() {
        TraceSpecification testSpec = new TraceSpecification("*=info:whitespace=all=disabled", null, false);
        TraceSpecification testSpec2 = new TraceSpecification("*=info:whitespace=all=disabled", null, true);
        assertFalse("Test specifications should compare NOT equal", testSpec.equals(testSpec2));
    }

    @Ignore
    @Test
    public void testUnmatchedSpecs() {
        TraceSpecification testSpec = new TraceSpecification("*=info: whitespace= all\n=\t disabled   : unknown=all", null, false);
        List<TraceElement> specs = testSpec.getSpecs();
        assertEquals("Test specification should contain two elements", 3, specs.size());
        specs.get(0).setMatched(true); // mark the first as matched

        testSpec.warnUnmatchedSpecs();
        assertTrue("Message should be written to messages.log", outputMgr.checkForMessages("TRAS0040I"));
        assertTrue("Message should contain whitespace element", outputMgr.checkForMessages("whitespace=all=disabled"));
        assertFalse("Message should NOT contain * element", outputMgr.checkForMessages("\\*=info"));

        outputMgr.copyMessageStream();
    }

}

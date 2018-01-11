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
package com.ibm.websphere.ras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.LoggingTestUtils;
import test.TestConstants;
import test.common.SharedOutputManager;

import com.ibm.websphere.ras.dummyinternal.DummyInternalClass;
import com.ibm.ws.logging.internal.TraceSpecification;
import com.ibm.ws.logging.internal.TraceSpecification.TraceElement;

/**
 *
 */
public class TrConfiguratorTest {
    static {
        LoggingTestUtils.ensureLogManager();
    }

    static SharedOutputManager outputMgr;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // make stdout/stderr "quiet"-- no output will show up for test
        // unless one of the copy methods or documentThrowable is called
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.logTo(TestConstants.BUILD_TMP);
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    @Before
    public void before() {
        // clear out the existing Tr class
        Tr.activeTraceSpec = new TraceSpecification("", null, false);
        Tr.allTraceComponents.clear();
    }
    @Test
    public void testBadTraceSpecification() {
        final String m = "testStaticTraceMethods";
        try {
            TrConfigurator.setTraceSpec("com.*=fred=enabled");

            TraceElement defaultSpec = Tr.activeTraceSpec.getSpecs().get(0);
            assertEquals("The generated spec should be the default (*)", "*", defaultSpec.groupName);
            assertEquals("The level sequence for the trace setting should be the default (6)", 6, defaultSpec.fineLevel);
            assertTrue("The action should be to enable trace", defaultSpec.action);
            assertTrue("Console output should contain TRAS0034W message", outputMgr.checkForStandardOut("TRAS0034W"));
            assertTrue("Message output should contain TRAS0034W message", outputMgr.checkForMessages("TRAS0034W"));
            assertFalse("Trace output should not contain TRAS0034W message", outputMgr.checkForTrace("TRAS0034W"));
            // We should skip the bad trace spec entirely.
            assertEquals(m + ": spec list length should be 1 - the default setting", 1, Tr.activeTraceSpec.getSpecs().size());

            outputMgr.resetStreams();

            // At the point the warning for this trace spec is produced, the previous invalid setting of
            // the trace spec is still in effect but we should have the default setting still so should
            // still get the console message
            TrConfigurator.setTraceSpec("com.*=all=fried");

            // We should skip the bad trace spec entirely.            
            assertEquals(m + ": spec list length should be 1 - the default setting", 1, Tr.activeTraceSpec.getSpecs().size());
            assertTrue("Console output should contain TRAS0034W message", outputMgr.checkForStandardOut("TRAS0035W"));
            assertTrue("Message output should contain TRAS0034W message", outputMgr.checkForMessages("TRAS0035W"));
            assertFalse("Trace output should not contain TRAS0034W message", outputMgr.checkForTrace("TRAS0035W"));

            outputMgr.resetStreams();

            // Set the same string again -- nothing should happen.. (no messages, as the string should not be reparsed)

            TrConfigurator.setTraceSpec("com.*=all=fried");
            assertEquals(m + ": spec list length should be 1 - the default setting", 1, Tr.activeTraceSpec.getSpecs().size());
            assertFalse("Console output should not contain TRAS0034W message", outputMgr.checkForStandardOut("TRAS0035W"));
            assertFalse("Message output should not contain TRAS0034W message", outputMgr.checkForMessages("TRAS0035W"));
            assertFalse("Trace output should not contain TRAS0034W message", outputMgr.checkForTrace("TRAS0035W"));

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Test with sensitive false first and then with true
     * since the TrConfigurator class state (package index) is kept between tests
     * and we do not know what order the tests will be executed.
     */
    @Test
    public void testSensitive() {
        final String m = "testSensitive";
        try {
            final Map<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put("suppressSensitiveTrace", false);

            TrConfigurator.update(newConfig);
            assertFalse("The suppressSensitiveTrace flag must be false in the trace spec.",
                        Tr.activeTraceSpec.isSensitiveTraceSuppressed());
            assertNull("The package index must not be set in the trace spec.",
                        Tr.activeTraceSpec.getSafeLevelsIndex());

            assertSensitiveFlagAndIndexAreSet(newConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
        	Tr.activeTraceSpec.getSafeLevelsIndex();
        }
    }

    /**
     * Same as above, but using a String "false" rather than a boolean.
     */
    @Test
    public void testSensitiveString() {
        final String m = "testSensitive";
        try {
            final Map<String, Object> newConfig = new HashMap<String, Object>();
            newConfig.put("suppressSensitiveTrace", "false");

            TrConfigurator.update(newConfig);
            assertFalse("The suppressSensitiveTrace flag must be false in the trace spec.",
                        Tr.activeTraceSpec.isSensitiveTraceSuppressed());
            assertNull("The package index must not be set in the trace spec.",
                        Tr.activeTraceSpec.getSafeLevelsIndex());

            assertSensitiveFlagAndIndexAreSet(newConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    /**
     * Stack products must use the update(Map) method to change the trace spec.
     * This test verifies that using this method will actually change the active
     * trace spec.
     */
    @Test
    public void testUpdateTraceSpec() {

        final TraceComponent tc = Tr.register(DummyInternalClass.class);
        final Map<String, Object> newConfig = new HashMap<String, Object>();

        // enable tracing for a specific component
        newConfig.put("traceSpecification", "com.ibm.websphere.ras.dummyinternal.*=all");
        TrConfigurator.update(newConfig);

        assertTrue("The update to traceSpec failed to enable tracing for the specifed component", tc.isDebugEnabled());

        // now reset the tracing to *=info
        newConfig.put("traceSpecification", "*=info");
        TrConfigurator.update(newConfig);

        assertFalse("The update to reset traceSpec failed to disable tracing for the specifed component", tc.isDebugEnabled());

    }

    private void assertSensitiveFlagAndIndexAreSet(final Map<String, Object> newConfig) {
        TrConfigurator.setSensitiveTraceListResourceName("test/properties/test.groups.ras.rawtracelist.properties");
        newConfig.put("suppressSensitiveTrace", true);
        TrConfigurator.update(newConfig);
        assertTrue("The suppressSensitiveTrace flag must be set in the trace spec.",
                   Tr.activeTraceSpec.isSensitiveTraceSuppressed());
        assertNotNull("The package index must be true in the trace spec.",
                      Tr.activeTraceSpec.getSafeLevelsIndex());
    }

}

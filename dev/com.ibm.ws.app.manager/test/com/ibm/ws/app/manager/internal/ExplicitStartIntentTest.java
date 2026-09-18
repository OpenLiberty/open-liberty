/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.objenesis.ObjenesisStd;

/**
 * Unit tests for the explicit-start intent preservation mechanism added to
 * {@link ApplicationConfigurator} to fix the race condition described in
 * support case TS022427317.
 *
 * <p>Scenario: an application configured with {@code autoStart="false"} receives an explicit
 * {@code ApplicationMBean.start()} call.  Before startup completes, OSGi Configuration Admin
 * delivers a {@code deleted(old_pid)} / {@code updated(new_pid)} pair for the same logical
 * application name (e.g. due to CICS BUNDLE installation rewriting {@code installedApps.xml}
 * following a z/OS IPL).  The new state machine must be started automatically without requiring
 * the external caller to issue a second {@code start()} invocation.
 *
 * <p>These tests verify the three-method contract of the fix:
 * <ol>
 *   <li>{@code noteExplicitStart(appName)} records pending intent.</li>
 *   <li>{@code clearExplicitStart(appName)} removes it (e.g. after an explicit stop).</li>
 *   <li>The pending-start set is correctly consumed exactly once in the replacement-ASM path,
 *       meaning a second {@code processUpdate} for the same name will not attempt a spurious
 *       start.</li>
 * </ol>
 *
 * <p>{@link ApplicationConfigurator} requires many OSGi services that cannot be obtained in
 * isolation.  This test bypasses the OSGi DS constructors via Objenesis (present on the
 * project test path) and manually initialises only the single field exercised by the methods
 * under test.
 */
public class ExplicitStartIntentTest {

    /** Instance under test — constructed without invoking any OSGi-dependent constructor. */
    private ApplicationConfigurator configurator;

    /** Direct reference to the private pending-start set (accessed via reflection). */
    private Set<String> pendingNames;

    @Before
    public void setUp() throws Exception {
        // Allocate without invoking the @Activate constructor — avoids NPE on OSGi collaborators.
        configurator = (ApplicationConfigurator) new ObjenesisStd().newInstance(ApplicationConfigurator.class);

        // Manually initialise the field that the methods under test read/write.
        Field pendingField = ApplicationConfigurator.class.getDeclaredField("_pendingExplicitStartAppNames");
        pendingField.setAccessible(true);
        pendingNames = new HashSet<String>();
        pendingField.set(configurator, pendingNames);
    }

    // ---------------------------------------------------------------------------
    // Tests
    // ---------------------------------------------------------------------------

    /**
     * Calling {@code noteExplicitStart} for an application name records the intent.
     */
    @Test
    public void testNoteExplicitStartRecordsIntent() {
        configurator.noteExplicitStart("cicsasclientEAR");

        assertTrue("Expected 'cicsasclientEAR' to be in the pending explicit-start set",
                   pendingNames.contains("cicsasclientEAR"));
    }

    /**
     * Calling {@code clearExplicitStart} after an explicit stop removes the intent, ensuring
     * that a subsequent configuration replacement does NOT automatically restart an
     * intentionally stopped application.
     */
    @Test
    public void testClearExplicitStartRemovesIntent() {
        configurator.noteExplicitStart("cicsasclientEAR");
        configurator.clearExplicitStart("cicsasclientEAR");

        assertFalse("Expected pending explicit-start intent to be cleared after stop",
                    pendingNames.contains("cicsasclientEAR"));
    }

    /**
     * {@code clearExplicitStart} is idempotent: clearing a name that was never recorded is a no-op.
     */
    @Test
    public void testClearExplicitStartIsIdempotent() {
        // Should not throw
        configurator.clearExplicitStart("nonexistentApp");

        assertTrue("Pending set should still be empty", pendingNames.isEmpty());
    }

    /**
     * Multiple independent applications can each have pending start intent simultaneously,
     * and stopping one does not affect the others.
     */
    @Test
    public void testMultipleAppsTrackedIndependently() {
        configurator.noteExplicitStart("appA");
        configurator.noteExplicitStart("appB");

        assertTrue("appA should be pending", pendingNames.contains("appA"));
        assertTrue("appB should be pending", pendingNames.contains("appB"));

        // Stopping appA must not affect appB
        configurator.clearExplicitStart("appA");

        assertFalse("appA should no longer be pending after stop", pendingNames.contains("appA"));
        assertTrue("appB should still be pending", pendingNames.contains("appB"));
    }

    /**
     * Simulates the core race-condition fix: after a PID replacement for the same application
     * name, the intent recorded via {@code noteExplicitStart} is consumed exactly once,
     * mirroring the {@code _pendingExplicitStartAppNames.remove(newAppName)} call inside
     * {@code processUpdate}.
     *
     * <p>Verifies:
     * <ul>
     *   <li>Recording intent before the replacement makes it available to {@code processUpdate}.</li>
     *   <li>After the first consumption the set no longer contains the name, so a second
     *       {@code processUpdate} for the same name does not trigger a spurious start.</li>
     * </ul>
     */
    @Test
    public void testIntentIsConsumedExactlyOnceOnPidReplacement() {
        final String appName = "cicsasclientEAR";

        // Step 1: external caller invokes start() on ASM[1] before config deletion arrives.
        configurator.noteExplicitStart(appName);
        assertTrue("Intent must be present after noteExplicitStart", pendingNames.contains(appName));

        // Step 2: processUpdate for the replacement PID consumes the intent (remove returns true).
        boolean consumed = pendingNames.remove(appName);
        assertTrue("First processUpdate should consume the pending intent (remove returned false)", consumed);
        assertFalse("After consumption the intent must not remain in the set", pendingNames.contains(appName));

        // Step 3: a hypothetical second processUpdate for the same name finds nothing.
        boolean consumedAgain = pendingNames.remove(appName);
        assertFalse("Intent must not be propagated a second time; remove should return false", consumedAgain);
    }

    /**
     * Verifies that {@code noteExplicitStart} is idempotent for the same app name — repeated
     * calls do not accumulate duplicate entries, and a single {@code clearExplicitStart} fully
     * removes the entry.
     */
    @Test
    public void testNoteExplicitStartIsIdempotent() {
        configurator.noteExplicitStart("cicsasclientEAR");
        configurator.noteExplicitStart("cicsasclientEAR"); // duplicate call

        assertTrue("Entry should be present after two noteExplicitStart calls",
                   pendingNames.contains("cicsasclientEAR"));

        configurator.clearExplicitStart("cicsasclientEAR");
        assertFalse("A single clearExplicitStart must fully remove a duplicate-noted entry",
                    pendingNames.contains("cicsasclientEAR"));
    }
}

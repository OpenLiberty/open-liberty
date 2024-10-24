/*******************************************************************************
 * Copyright (c) 2017, 2024 IBM Corporation and others.
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
package com.ibm.ws.jca.fat.derbyra;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.checkpoint.spi.CheckpointPhase;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                DerbyResourceAdapterTest.class,
                DerbyResourceAdapterSecurityTest.class,
                LoginModuleInStandaloneResourceAdapterTest.class,
                DerbyRACheckpointLimitationsTest.class,
                DerbyRACheckpointTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(new JakartaEE9Action().fullFATOnly())
                            .andWith(new JakartaEE10Action());
        } else {
            repeat = RepeatTests.with(new EmptyAction().fullFATOnly())
                            .andWith(new JakartaEE9Action());
        }
    }

    // MOCK CHECKPOINT SUPPORT
    // Command line options "--internal-checkpoint-at=before/afterAppStart" and system
    // property "io.openliberty.checkpoint.stub.criu" enable the server to stub-out
    // calls to criu operations during checkpoint and restore.

    // Use mock support for tests that don't care about criu and jvm criu function,
    // or for debugging a test server over checkpoint and restore in the same process.
    static final List<String> INTERNAL_CHECKPOINT_INACTIVE = Collections.emptyList();
    static final List<String> INTERNAL_CHECKPOINT_BEFORE_APP_START = Arrays.asList("--internal-checkpoint-at=beforeAppStart");
    static final List<String> INTERNAL_CHECKPOINT_AFTER_APP_START = Arrays.asList("--internal-checkpoint-at=afterAppStart");

    /*
     * Enable stubbed criu operations within checkpoint-and restore at the specified
     * checkpoint phase.
     */
    public static void setMockCheckpoint(LibertyServer server, CheckpointPhase checkpointPhase) throws Exception {
        Map<String, String> jvmOptions = server.getJvmOptionsAsMap();
        switch (checkpointPhase) {
            case BEFORE_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(INTERNAL_CHECKPOINT_BEFORE_APP_START);
                break;
            case AFTER_APP_START:
                jvmOptions.put("-Dio.openliberty.checkpoint.stub.criu", "true");
                server.setExtraArgs(INTERNAL_CHECKPOINT_AFTER_APP_START);
                break;
            default:
                // Normal server operation; no checkpoint-restore
                jvmOptions.remove("-Dio.openliberty.checkpoint.stub.criu");
                server.setExtraArgs(INTERNAL_CHECKPOINT_INACTIVE);
        }
        server.setJvmOptions(jvmOptions);
    }

}
/*******************************************************************************
 * Copyright (c) 2018, 2023 IBM Corporation and others.
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
package test.server.transport.http2;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;

/**
 * HTTP/2 Tests
 *
 * Make sure to add any new test classes to the @SuiteClasses
 * annotation.
 *
 * Make sure to distinguish FULL mode tests using
 * <code>@Mode(TestMode.FULL)</code>. Tests default to
 * use LITE mode (<code>@Mode(TestMode.LITE)</code>).
 *
 * By default only LITE mode tests are run. To also run
 * full mode tests a property must be specified:
 *
 * -Dfat.test.mode=FULL.
 *
 */
@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class, // LITE
                Http2LiteModeTests.class, // LITE
                Http2SecureTests.class, // LITE
                Http2FullModeTests.class, // FULL
                /** move tests to Http2FullTracingTests to diagnose failures via full tracing */
                Http2FullTracingTests.class, // FULL
                Http2Config40H2Off.class, // FULL
                Http2Config31H2Off.class, // FULL
                Http2Config31H2On.class, // FULL
                Http2WindowUpdateTests.class, //FULL
                Http2CompressionTests.class //FULL
//MultiSessionTests.class // FULL; disabled for now
})

public class FATSuite {
    private static final Class<?> c = FATSuite.class;

    // Run with no modifications to the server besides enabling Beta and running netty
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.BETA_OPTION());

}

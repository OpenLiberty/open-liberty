/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import io.openliberty.transport.http_fat.accesslists.AccessListsTests;

@RunWith(Suite.class)
@SuiteClasses({
                AccessLogRolloverTest.class,
                AccessListsTests.class,
                MaxOpenConnectionsTests.class,
                PortOpenRetriesTests.class,
                SoLingerTests.class,
                SoReuseAddrTests.class,
                TcpOptionsDefaultTests.class
})

public class FATSuite {

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE6_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11))
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17))
                    .andWith(FeatureReplacementAction.EE11_FEATURES());
}

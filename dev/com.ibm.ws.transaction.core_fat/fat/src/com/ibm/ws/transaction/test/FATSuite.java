/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                SimpleTest.class,
                OnePCDisabledTest.class,
                XATest.class,
                XAFlowTest.class,
                RecoveryTest.class,
                WaitForRecoveryTest.class,
                TransactionScopedObserversTest.class
})
public class FATSuite {

    private static LibertyServer server1 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction");
    private static LibertyServer server2 = LibertyServerFactory.getLibertyServer("com.ibm.ws.transaction_waitForRecovery");
    // Using the RepeatTests @ClassRule will cause all tests to be run twice.
    // First without any modifications, then again with all features upgraded to their EE8 equivalents.
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification()
                    .andWith(FeatureReplacementAction.EE8_FEATURES());
}

/*******************************************************************************
 * Copyright (c) 2011, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jca.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jca.fat.app.ConnectionManagerMBeanTest;
import com.ibm.ws.jca.fat.app.DependantApplicationTest;
import com.ibm.ws.jca.fat.app.JCATest;
import com.ibm.ws.jca.fat.regr.InboundSecurityTest;
import com.ibm.ws.jca.fat.regr.InboundSecurityTestRapid;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                DependantApplicationTest.class,
                JCATest.class,
                ConnectionManagerMBeanTest.class,
                InboundSecurityTest.class,
                InboundSecurityTestRapid.class
})
public class FATSuite {

    public static final String javaeeServer = "com.ibm.ws.jca.fat";
    public static final String jakartaeeServer = "com.ibm.ws.jca.fat.jakarta";

    // Need widen option to handle jar file within a jar file for EE9/EE10/EE11.
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(FeatureReplacementAction.NO_REPLACEMENT())
                    .andWith(FeatureReplacementAction.EE8_FEATURES().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_11).withWiden())
                    .andWith(FeatureReplacementAction.EE10_FEATURES().conditionalFullFATOnly(FeatureReplacementAction.GREATER_THAN_OR_EQUAL_JAVA_17).withWiden())
                    .andWith(FeatureReplacementAction.EE11_FEATURES().withWiden());

    public static LibertyServer getServer() {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            return LibertyServerFactory.getLibertyServer(jakartaeeServer);
        } else {
            return LibertyServerFactory.getLibertyServer(javaeeServer);
        }

    }
}

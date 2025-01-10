/*******************************************************************************
 * Copyright (c) 2011, 2025 IBM Corporation and others.
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

import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
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

    @ClassRule
    public static RepeatTests repeat;

    static {
        // EE10 requires Java 11.  If we only specify EE10 for lite mode it will cause no tests to run which causes an error.
        // If we are running on Java 8 have EE9 be the lite mode test to run.
        if (JavaInfo.JAVA_VERSION >= 11) {
            repeat = RepeatTests.with(new EmptyAction())
                            .andWith(new EE8FeatureReplacementAction().fullFATOnly())
                            .andWith(new JakartaEE9Action().fullFATOnly().withWiden()) // need widen option to handle jar file within a jar file.
                            .andWith(new JakartaEE10Action().withWiden()); // need widen option to handle jar file within a jar file.
        } else {
            repeat = RepeatTests.with(new EmptyAction())
                            .andWith(new EE8FeatureReplacementAction().fullFATOnly())
                            // need widen option to handle jar file within a jar file.
                            .andWith(new JakartaEE9Action().withWiden());
        }

    }

    public static LibertyServer getServer() {
        if (JakartaEEAction.isEE9OrLaterActive()) {
            return LibertyServerFactory.getLibertyServer(jakartaeeServer);
        } else {
            return LibertyServerFactory.getLibertyServer(javaeeServer);
        }

    }
}

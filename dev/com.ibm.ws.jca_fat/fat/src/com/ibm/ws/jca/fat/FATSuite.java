/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import componenttest.rules.repeater.EmptyAction;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.JakartaEE9Action;
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

    /*
     * EE7 will run in full mode only.
     * EE9 will run in full mode only.
     * EE10 will run in full and lite mode.
     */
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
                    // need widen option to handle jar file within a jar file.
                    .andWith(new JakartaEE9Action().withWiden())
                    // need widen option to handle jar file within a jar file.
                    .andWith(new JakartaEE10Action().withWiden());

    public static LibertyServer getServer() {
        if (JakartaEE9Action.isActive() || JakartaEE10Action.isActive()) {
            return LibertyServerFactory.getLibertyServer(jakartaeeServer);
        } else {
            return LibertyServerFactory.getLibertyServer(javaeeServer);
        }

    }
}

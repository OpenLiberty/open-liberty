/*******************************************************************************
 * Copyright (c) 2011, 2021 IBM Corporation and others.
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
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@RunWith(Suite.class)
@SuiteClasses({
                DependantApplicationTest.class,
                JCATest.class,
                ConnectionManagerMBeanTest.class,
                InboundSecurityTest.class,
                InboundSecurityTestRapid.class
})
public class FATSuite {

    /*
     * EE7 will run with full fat only. EE9 will be run with lite and full fat.
     */
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new EmptyAction().fullFATOnly())
                    .andWith(FeatureReplacementAction.EE9_FEATURES());

    public static void addServerVariables(LibertyServer server) {
        if (JakartaEE9Action.isActive()) {
            server.addEnvVar("TOPIC", "jakarta.jms.Topic");
            server.addEnvVar("PASSWORD_CRED", "jakarta.resource.spi.security.PasswordCredential");
        } else {
            server.addEnvVar("TOPIC", "javax.jms.Topic");
            server.addEnvVar("PASSWORD_CRED", "javax.resource.spi.security.PasswordCredential");
        }

    }

}

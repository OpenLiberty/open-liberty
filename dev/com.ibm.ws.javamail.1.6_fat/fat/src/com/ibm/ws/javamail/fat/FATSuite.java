/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.javamail.fat;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                IMAPTest.class,
                POP3Test.class,
                SMTPTest.class,
                MailSessionInjectionTest.class,
                JavaMailResourceMBeanTest.class
})
public class FATSuite {

    private static LibertyServer mailSesionServer = LibertyServerFactory.getLibertyServer("mailSessionTestServer");

    @BeforeClass
    public static void setupApp() throws Exception {
        ShrinkHelper.defaultApp(mailSesionServer, "TestingApp", "TestingApp.*");

    }

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification().andWith(FeatureReplacementAction.EE8_FEATURES().withID("javaMail-1.6"));

}

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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                IMAPTest.class,
                POP3Test.class,
                SMTPTest.class,
                MailSessionInjectionTest.class
})
public class FATSuite {

    private static LibertyServer mailSesionServer = LibertyServerFactory.getLibertyServer("mailSessionTestServer");

    private static final Class<?> c = FATSuite.class;
    @ClassRule
    public static RepeatTests r = componenttest.rules.repeater.RepeatTests.withoutModification()
                    .andWith(new JakartaEE9Action());

    @BeforeClass
    public static void setupApp() throws Exception {
        ShrinkHelper.defaultApp(mailSesionServer, "TestingApp", "TestingApp.*");
        if (JakartaEE9Action.isActive()) {
            Log.info(c, "setUpApp", "Transforming greenmail jar to Jakarta-EE-9: ");
            Log.info(c, "setupApp", "Current working directory =" + System.getProperty("user.dir"));
            Path greenmailJar = Paths.get(System.getProperty("user.dir"), "lib", "greenmail-1.5.10.jar");
            Log.info(c, "setupApp", "Greenmail path =" + greenmailJar);
            JakartaEE9Action.transformApp(greenmailJar);
        }

    }
}

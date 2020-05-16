/*******************************************************************************
 * Copyright (c) 2017,2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jdbc.fat.v41;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(Suite.class)
@SuiteClasses({
                JDBC41UpgradeTest.class,
                JDBC41Test.class
})
public class FATSuite {
    public static final String appName = "basicfat";
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("com.ibm.ws.jdbc.fat.v41");

    @ClassRule
    public static RepeatTests r = RepeatTests
                    .withoutModification()
                    ;// TODO .andWith(new JakartaEE9Action());

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, appName + ".war")
                        .addPackage("jdbc.fat.v41.web");
        ShrinkHelper.exportAppToServer(server, app);

        JavaArchive slowdriver = ShrinkWrap.create(JavaArchive.class, "slowdriver.jar")
                        .addPackage("jdbc.fat.v41.slowdriver");
        ShrinkHelper.exportToServer(server, "derby", slowdriver);
    }
}
/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config12.test;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.ws.microprofile.config12.converter.implicit.web.ImplicitConverterServlet;
import com.ibm.ws.microprofile.config12.converter.priority.web.ConverterPriorityServlet;
import com.ibm.ws.microprofile.config12.converter.type.web.TypeConverterServlet;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.annotation.TestServlets;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.MicroProfileActions;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * Example Shrinkwrap FAT project:
 * <li> Application packaging is done in the @BeforeClass, instead of ant scripting.
 * <li> Injects servers via @Server annotation. Annotation value corresponds to the
 * server directory name in 'publish/servers/%annotation_value%' where ports get
 * assigned to the LibertyServer instance when the 'testports.properties' does not
 * get used.
 * <li> Specifies an @RunWith(FATRunner.class) annotation. Traditionally this has been
 * added to bytecode automatically by ant.
 * <li> Uses the @TestServlet annotation to define test servlets. Notice that no @Test
 * methods are defined in this class. All of the @Test methods are defined on the test
 * servlet referenced by the annotation, and will be run whenever this test class runs.
 */
@RunWith(FATRunner.class)
public class Config12ConverterTests extends FATServletClient {

    public static final String SERVER_NAME = "ConverterServer";
    public static final String APP_NAME = "converterApp";

    @ClassRule
    public static RepeatTests r = MicroProfileActions.repeat(SERVER_NAME, MicroProfileActions.MP50, MicroProfileActions.MP41, MicroProfileActions.MP13);

    @Server(SERVER_NAME)
    @TestServlets({
                    @TestServlet(servlet = ConverterPriorityServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = ImplicitConverterServlet.class, contextRoot = APP_NAME),
                    @TestServlet(servlet = TypeConverterServlet.class, contextRoot = APP_NAME) })
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        DeployOptions[] options = { DeployOptions.SERVER_ONLY };
        ShrinkHelper.defaultDropinApp(server, APP_NAME, options, "com.ibm.ws.microprofile.config12.converter.*");

        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMCG0017E"); //stringConstructorMissingTest expects implicit.string.constructor.method.not.found.CWMCG0017E
    }

}

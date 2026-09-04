/*******************************************************************************
 * Copyright (c) 2024, 2026 IBM Corporation and others.
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
package io.openliberty.classloading.base.fat;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Tests that Spring can hook into the Liberty app classloaders to perform their byte code processing.
 */
@org.junit.runner.RunWith(componenttest.custom.junit.runner.FATRunner.class)
public class SpringLoaderTest {

    private static final LibertyServer _server = LibertyServerFactory.getLibertyServer("classloader_spring_FAT");

    @BeforeClass
    public static void beforeClass() throws Exception {
        WebArchive war = ShrinkHelper.buildDefaultApp("spring-loader-sci.war", "com.ibm.test.spring");
        ShrinkHelper.addDirectory(war, "test-applications/spring-loader-sci.war/resources");
        ShrinkHelper.exportDropinAppToServer(_server, war, DeployOptions.SERVER_ONLY);
        _server.startServer("SpringLoaderTest.log");
        _server.waitForStringInLog("MyServlet");
    }

    @AfterClass
    public static void afterClass() throws Exception {
        _server.stopServer(true);
        _server.uninstallSystemFeature("classloadingexposelibertyinternals-1.0");
    }

    @Test
    public void checkSpringCanHookIntoAppClassLoader() throws Exception {
        testSpringHook("this.", 1);
    }

    @Test
    public void checkSpringCanHookIntoThreadContextClassLoader() throws Exception {
        testSpringHook("tccl.", 2);
    }

    private void testSpringHook(String classLoaderString, int num) throws Exception {
        List<String> messages = _server.findStringsInLogs("SpringLoaderHookTestSCI: " + classLoaderString + "classloader - loading com.ibm.test.spring.MyDummyClass" + num);
        assertFalse("No indication that the ServletContextInitializer executed", messages.isEmpty());

        messages = _server.findStringsInLogs("MyTransformer - " + classLoaderString + "loader=.* className=.*MyDummyClass" + num);
        assertFalse("ClassFileTransformer not invoked", messages.isEmpty());

        messages = _server.findStringsInLogs("SpringLoaderHookTestSCI: " + classLoaderString + "classloader - loaded .*MyDummyClass" + num);
        assertFalse("ClassLoader failed to load dummy class with Spring hook", messages.isEmpty());

        messages = _server.findStringsInLogs("SpringLoaderHookTestSCI: " + classLoaderString + "getThrowawayClassLoader = ");
        assertFalse("Failed to get throwaway classloader", messages.isEmpty());

        // ensure that throwaway classloader is not null
        for (String msg : messages) {
            assertFalse("null was returned for getThrowawayClassloader() call", msg.endsWith("null"));
        }
    }

}

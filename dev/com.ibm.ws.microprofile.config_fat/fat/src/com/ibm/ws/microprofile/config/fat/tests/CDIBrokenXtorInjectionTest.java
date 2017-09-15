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
package com.ibm.ws.microprofile.config.fat.tests;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;

/**
 *
 */
public class CDIBrokenXtorInjectionTest extends LoggingTest {

    @ClassRule
    public static SharedServer SHARED_SERVER = new SharedServer("brokenCDIConfigServer");

    @Override
    protected SharedServer getSharedServer() {
        return SHARED_SERVER;
    }

    @Test
    public void testConstructorUnnamed() throws Exception {
        List<String> errors = getSharedServer().getLibertyServer().findStringsInLogs("ConfigUnnamedConstructorInjectionBean.*The property name must be specified for Constructor and Method configuration property injection");
        assertTrue(errors.size() > 0);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SHARED_SERVER != null && SHARED_SERVER.getLibertyServer().isStarted()) {
            /*
             * Ignore following exception as those are expected:
             * CWMCG5002E: The property name must be specified for Constructor and Method configuration property injection.
             *
             * CWWKZ0002E: An exception occurred while starting the application brokenCDIConffig. The exception message was:
             * com.ibm.ws.container.service.state.StateChangeException: org.jboss.weld.exceptions.DeploymentException:
             */
            SHARED_SERVER.getLibertyServer().stopServer("CWWKZ0002E", "CWMCG5003E");
        }
    }
}

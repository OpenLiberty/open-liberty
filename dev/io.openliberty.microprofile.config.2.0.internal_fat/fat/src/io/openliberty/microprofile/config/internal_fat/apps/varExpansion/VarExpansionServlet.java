/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.varExpansion;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;

/**
 * Servlet for VarExpansionTest.
 *
 * Tests that server.xml {@code <variable>} list expansion (via the
 * {@code list()} function) is correctly surfaced through MicroProfile Config.
 */
@WebServlet("/VarExpansionServlet")
public class VarExpansionServlet extends FATServlet {

    private static final long serialVersionUID = 1L;

    @Inject
    ConfigChecker configChecker;

    /**
     * This is a variant on the same test in com.ibm.ws.microprofile.config.1.3_fat
     * that covers the new method getOptionalValues in mpConfig 2.0
     */
    @Test
    public void testAppPropertyExpansionOfList() throws Exception {

        configChecker.assertConfigPropertyEquals("app.appPropertyExamplePorts", new String[] { "27017" });
        configChecker.assertConfigPropertyEquals("app.appPropertyExampleHosts", new String[] { "mongo1.example.com" });

        //Now check they also work when we request mpConfig provides in list form

        //The fact mpConfig returns a different value for List and Array is almost certainly wrong, but for now this test
        //will match the existing behavior

        //These full lists come from io.smallrye.config.ConfigValueConfigSourceWrapper
        List<String> portsList = Arrays.asList(new String[] { "27017", "27018", "27019" });
        List<String> hostsList = Arrays.asList(new String[] { "mongo1.example.com", "mongo2.example.com", "mongo3.example.com" });

        configChecker.assertConfigPropertyEquals("examplePorts", portsList);
        configChecker.assertConfigPropertyEquals("exampleHosts", hostsList);
    }

    /**
     * This is a variant on the same test in com.ibm.ws.microprofile.config.1.3_fat
     * that covers the new method getOptionalValues in mpConfig 2.0
     */
    @Test
    public void testDirectVariableExpansionOfList() throws Exception {

        //This method's variables are coming from ServerXMLVariableConfigSource.getProperties without needing
        //any extra code for handling list expansion

        String[] ports = new String[] { "27017", "27018", "27019" };
        String[] hosts = new String[] { "mongo1.example.com", "mongo2.example.com", "mongo3.example.com" };

        configChecker.assertConfigPropertyEquals("examplePorts", ports);
        configChecker.assertConfigPropertyEquals("exampleHosts", hosts);

        //Now check they also work when we request mpConfig provides in list form

        configChecker.assertConfigPropertyEquals("examplePorts", Arrays.asList(ports));
        configChecker.assertConfigPropertyEquals("exampleHosts", Arrays.asList(hosts));
    }

}

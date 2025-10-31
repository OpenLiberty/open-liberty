/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat.accesslists;

import java.io.StringReader;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestName;

import app1.web.ClientInfo;
import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;

/**
 * This class is a common superclass of all Http FAT tests and can be used to
 * hold instance/test centric common methods and state.
 */
public class HttpTest extends FATServletClient {

    /* This server is used to get the hostname, address and port that is the test client */
    @Server("ClientInfo")
    @TestServlet(servlet = ClientInfo.class, contextRoot = "ClientInfo")
    public static LibertyServer clientInfoServer;

    public static Properties clientDetails = new Properties();
    public static String app = "app1"; // as a default

    /* Useful for getting hold of the current test */
    @Rule
    public TestName test = new TestName();

    /**
     * E.g. AccessListsTests_testA11
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "_" + test.getMethodName();
    }

    /**
     * Retrieve the test client hostname, address and port
     *
     * @return Properties that contain template substitutions
     */
    static Properties setClientInfo() {
        try {
            clientInfoServer.startServer(true, true);
            String info = Utils.get(clientInfoServer, "/" + app + "/ClientInfo", "", "CLIENT_HOST=", "");
            clientInfoServer.stopServer();
            clientDetails.load(new StringReader(info));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
        return clientDetails;
    }

    /**
     * Check that we can get a response from the server
     *
     * @param server
     * @throws Exception
     */
    public String checkAccessAllowed(LibertyServer server) throws Exception {
        return Utils.accessAllowed(server, "/" + app + "/Backend", "", "Backend");
    }

    /**
     * Check that we get connection refused
     *
     * @param server
     * @throws Exception
     */
    public void checkAccessDenied(LibertyServer server) throws Exception {
        Utils.accessDenied(server, "/" + app + "/Backend", "", "Backend");
    }

    /**
     * Customize the servers server.xml file for this particular test
     *
     * @param server
     */
    public void setUpServer(LibertyServer server) {
        Utils.setUpServer(server, this);
    }

    /**
     * This method allows code to be copied directly from tWas tests into HttpTests
     * without having to put "Utils." in front of every call
     */
    protected String putInWildCards(String insert, int i, String string) {
        return Utils.putInWildCards(insert, i, string);
    }

    /**
     * This method allows code to be copied directly from tWas tests into HttpTests
     * without having to put "Utils." in front of every call
     */
    public static String convertIP4toIP6(String ip4) {
        return Utils.convertIP4toIP6(ip4);
    }

    /**
     * Simple logging primarily for unit testing etc.
     *
     * @param string
     */
    protected void debug(String string) {
        Utils.debug(string);
    }
}

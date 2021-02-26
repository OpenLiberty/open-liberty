/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.transaction.test.tests;

import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.rules.TestName;

import com.ibm.tx.jta.ut.util.XAResourceImpl;
import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpUtils;

/**
 *
 */
public class XATestBase {

    LibertyServer _server;
    String _app;
    String _servlet;

    /**
     * @param server
     * @param servletName
     * @param appName
     */
    public XATestBase(String appName, String servletName) {
        _app = appName;
        _servlet = servletName;
    }

    /**
     * @param server
     * @param appName
     * @throws Exception
     */
    public void setup(LibertyServer server) throws Exception {
        _server = server;

        ShrinkHelper.defaultApp(_server, _app, "com.ibm.ws.transaction.web.*");

        // TODO: Revisit this after all features required by this FAT suite are available.
        // The test-specific public features, txtest-x.y, are not in the repeatable EE feature
        // set. And, the ejb-4.0 feature is not yet available. Enable jdbc-4.2 to enable transactions-2.0.
        // The following sets the appropriate features for the EE9 repeatable tests.
        if (JakartaEE9Action.isActive()) {
            _server.changeFeatures(Arrays.asList("jdbc-4.2", "txtest-2.0", "servlet-5.0", "componenttest-2.0", "osgiconsole-1.0", "jndi-1.0"));
        }

        server.setServerStartTimeout(300000);
        _server.startServer();
    }

    /**
     * @param server
     * @throws Exception
     */
    public void tearDown() throws Exception {
        _server.stopServer("WTRN0075W", "WTRN0076W"); // Stop the server and indicate the '"WTRN0075W", "WTRN0076W" error messages were expected
        ShrinkHelper.cleanAllExportedArchives();
    }

    /**
     * @param server
     * @param servletName
     * @param testName
     * @throws Exception
     */
    public void testSetTransactionTimeoutReturnsTrue(TestName testName) throws Exception {
        _server.setMarkToEndOfLog();
        HttpUtils.findStringInReadyUrl(_server, FATServletClient.getPathAndQuery(_servlet, testName.getMethodName()), FATServletClient.SUCCESS);
        assertNotNull(_server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): TRUE"),
                      "setTransactionTimeout() does not seem to have been called");
    }

    /**
     * @param server
     * @param servletName
     * @param testName
     * @throws Exception
     */
    public void testSetTransactionTimeoutReturnsFalse(TestName testName) throws Exception {
        _server.setMarkToEndOfLog();
        HttpUtils.findStringInReadyUrl(_server, FATServletClient.getPathAndQuery(_servlet, testName.getMethodName()), FATServletClient.SUCCESS);
        assertNotNull(_server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): FALSE"),
                      "setTransactionTimeout() does not seem to have been called");
    }

    /**
     * @param server
     * @param servletName
     * @param testName
     * @throws Exception
     */
    public void testSetTransactionTimeoutThrowsException(TestName testName) throws Exception {
        _server.setMarkToEndOfLog();
        HttpUtils.findStringInReadyUrl(_server, FATServletClient.getPathAndQuery(_servlet, testName.getMethodName()), FATServletClient.SUCCESS);
        assertNotNull(_server.waitForStringInLogUsingMark(XAResourceImpl.class.getCanonicalName() + ".setTransactionTimeout\\([0-9]*\\): javax.transaction.xa.XAException"),
                      "setTransactionTimeout() does not seem to have been called");
    }
}

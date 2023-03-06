/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * WLP Copyright IBM Corp. 2015
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
package com.ibm.ws.transport.iiop;

import org.jboss.shrinkwrap.api.Archive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.TestServlet;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.corba.web.war.MyIIOPClientServlet;

@RunWith(FATRunner.class)
public class IIOPClientServletTest extends FATServletClient {

	@Server("buckyball")
	@TestServlet(servlet = MyIIOPClientServlet.class, contextRoot = "test.corba.web")
	public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
        for (Archive<?> app: FATSuite.SERVER_APPS) ShrinkHelper.exportDropinAppToServer(server, app);
        server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
	server.stopServer();
    }
}

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

import static org.junit.Assert.fail;

import java.rmi.AccessException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.iiop.common.HelloService;



@RunWith(FATRunner.class)
public class InterceptorFatTest extends FATServletClient {

	@Server("bouncyball")
	public static LibertyServer server;

	@BeforeClass
    public static void beforeClass() throws Exception {
		server.installSystemBundle("test.iiop.interceptor");
	server.installSystemFeature("test.iiop.interceptor-1.0");
	server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
	server.stopServer();
    }

    private static ORB orbToBeDestroyed;

    private static String lastConfigFile;

    public static ORB getOrb() {
        discardOrb();
        return orbToBeDestroyed = RemoteClientUtil.initOrb(null);
    }

    @AfterClass
    public static void discardOrb() {
        if (orbToBeDestroyed == null)
            return;
        orbToBeDestroyed.shutdown(true);
        orbToBeDestroyed.destroy();
    }

    private void changeServerConfig(final String configFile) throws Exception {
        System.out.println("### changingServerConfig from " + lastConfigFile + " to " + configFile);
        if (configFile.equals(lastConfigFile)) {
            System.out.println("### early return");
            return;
        }
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(configFile);
        lastConfigFile = configFile;
        Assert.assertNotNull(server.waitForStringInLogUsingMark("CWWKG001[78]I"));
    }

    @Test
    public void testSimpleMethodCall() throws Exception {
        changeServerConfig("bouncyball.allowrequest.server.xml");
        RemoteClientUtil.getRemoteServiceFromServerLog(server, getOrb(), HelloService.class).sayHello();
    }

    /**
     * Try calling sayHello() with a new interceptor installed that
     * raises a {@link NO_PERMISSION} exception. This should result in a
     * an {@link AccessException} on the client since the ORB will covert
     * the CORBA exception into am RMI exception.
     */
    @Test
    public void testVetoedMethodCall() throws Throwable {
        try {
            changeServerConfig("bouncyball.NO_PERMISSION.server.xml");
            RemoteClientUtil.getRemoteServiceFromServerLog(server, getOrb(), HelloService.class).sayHello();
            fail("Expected an AccessException");
        } catch (AccessException expected) {
            try {
                throw expected.detail;
            } catch (NO_PERMISSION expectedCause) {
            }
        }
    }

    @Test
    public void testInterceptorComingAndGoing() throws Throwable {
        testSimpleMethodCall();
        testVetoedMethodCall();
        testSimpleMethodCall();
        testVetoedMethodCall();
    }
}

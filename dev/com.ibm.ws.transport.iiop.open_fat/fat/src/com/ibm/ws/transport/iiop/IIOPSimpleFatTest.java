/*******************************************************************************
 * Copyright (c) 2014-2023 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop;

import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.Properties;

import javax.rmi.PortableRemoteObject;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import componenttest.annotation.MaximumJavaLevel;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.iiop.client.ClientCallbackServiceImpl;
import test.iiop.client._ClientCallbackServiceImpl_Tie;
import test.iiop.common.ClientCallbackService;
import test.iiop.common.HelloService;
import test.iiop.common.ServerCallbackService;
import test.iiop.notcommon.VersionedException;


@RunWith(FATRunner.class)
public class IIOPSimpleFatTest extends FATServletClient {

	@Server("buckyball")
	public static LibertyServer server;

    @BeforeClass
    public static void beforeClass() throws Exception {
		server.startServer();
    }

    @AfterClass
    public static void afterClass() throws Exception {
	server.stopServer();
    }

    private static Properties props(String... args) {
        Properties result = new Properties();
        for (int i = 0; i < args.length; i += 2)
            result.put(args[i], args[i + 1]);
        return result;
    }

    @Test
    public void testHelloServiceRemoteCall() throws Exception {
        final ORB orb = initOrb(props());

        try {
            HelloService helloService = RemoteClientUtil.getRemoteServiceFromServerLog(server, orb, HelloService.class);
            System.out.println("### Calling remote HelloService");
            helloService.sayHello();

            String s = server.waitForStringInLog("Hello, world! I am your humble servant");
            Assert.assertNotNull(s);
        } finally {
            orb.shutdown(true);
            orb.destroy();
        }
    }

    @Test(expected = VersionedException.class)
    @Ignore
    @MaximumJavaLevel(javaLevel = 8)
    public void testVersionedException() throws Throwable {
//        Uncomment to get IBM ORB debug and comm trace
//        final ORB orb = initOrb(props("com.ibm.CORBA.Debug", "true", "com.ibm.CORBA.CommTrace", "true"));
        final ORB orb = initOrb(props());

        try {
            String nsURL = RemoteClientUtil.getNameServiceUrl(server);
            String ejbURL = nsURL + "#ejb/global/testCallback/testCallbackEjb/TestCallbackEjb!test%5c.iiop%5c.common%5c.ServerCallbackService";
            Object stub = orb.string_to_object(ejbURL);
            ServerCallbackService scs = (ServerCallbackService) PortableRemoteObject.narrow(stub, ServerCallbackService.class);
            System.out.println("### Calling remote ServerCallbackService");
            scs.throwRuntimeException(null);
        } catch (ServerException se) {
            RemoteException re = (RemoteException) se.getCause();
            throw re.getCause();
        } finally {
            orb.shutdown(true);
            orb.destroy();
        }
    }

    @Test(expected = VersionedException.class)
    @Ignore
    public void testVersionedExceptionFromCallback() throws Throwable {
//        Uncomment to get IBM ORB debug and comm trace
//        final ORB orb = initOrb(props("com.ibm.CORBA.Debug", "true", "com.ibm.CORBA.CommTrace", "true"));
        final ORB orb = initOrb(props());

        try {
            // create and export a callback service
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            rootPOA.the_POAManager().activate();
            ClientCallbackServiceImpl ccsi = new ClientCallbackServiceImpl();

            _ClientCallbackServiceImpl_Tie tie = new _ClientCallbackServiceImpl_Tie();
            tie.setTarget(ccsi);

            rootPOA.activate_object(tie);
            ClientCallbackService ccs = (ClientCallbackService) PortableRemoteObject.narrow(tie.thisObject(), ClientCallbackService.class);

            String nsURL = RemoteClientUtil.getNameServiceUrl(server);
            String ejbURL = nsURL + "#ejb/global/testCallback/testCallbackEjb/TestCallbackEjb!test%5c.iiop%5c.common%5c.ServerCallbackService";
            Object stub = orb.string_to_object(ejbURL);
            ServerCallbackService scs = (ServerCallbackService) PortableRemoteObject.narrow(stub, ServerCallbackService.class);
            System.out.println("### Calling remote ServerCallbackService");
            scs.throwRuntimeException(ccs);
        } catch (ServerException se) {
            RemoteException re = (RemoteException) se.getCause();
            throw re.getCause();
        } finally {
            orb.shutdown(true);
            orb.destroy();
        }
    }

    private static ORB initOrb(Properties props, String... args) {
        return ORB.init(args, props);
    }
}

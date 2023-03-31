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

import static org.junit.Assert.assertNotNull;

import javax.rmi.PortableRemoteObject;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.iiop.common.CosNamingChecker;
import test.iiop.common.util.NamingUtil;

@RunWith(FATRunner.class)
public abstract class AbstractCosNamingFatTest extends FATServletClient{
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

    private static ORB orb;
    private static CosNamingChecker cnc;
    private static String nsUrl;
    private static NamingContext rootContext;

    protected static void setupUsingStringToObject() throws Exception {
        nsUrl = RemoteClientUtil.getNameServiceUrl(server);
        orb = RemoteClientUtil.initOrb(null);
        rootContext = NamingContextHelper.narrow(orb.string_to_object(nsUrl));
        cnc = RemoteClientUtil.getRemoteServiceFromServerLog(server, orb, CosNamingChecker.class);
    }

    protected static void setupUsingORBInitRef() throws Exception {
        nsUrl = RemoteClientUtil.getNameServiceUrl(server);
        orb = RemoteClientUtil.initOrb(null, "-ORBInitRef", "NameService=" + nsUrl);
        rootContext = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
        org.omg.CORBA.Object cncObj = rootContext.resolve(NamingUtil.CNC_NAME);
        cnc = (CosNamingChecker) PortableRemoteObject.narrow(cncObj, CosNamingChecker.class);
    }

    public static void destroyORB() {
        orb.destroy();
    }

    @Test
    public void testNameServiceIsAvailableOnServer() throws Exception {
        cnc.checkNameServiceIsAvailable();
    }

    @Test
    public void testNameServiceIsAvailableRemotely() throws Exception {
        org.omg.CORBA.Object obj = orb.string_to_object(nsUrl);
        NamingContextExt ctx = NamingContextExtHelper.narrow(obj);
        Assert.assertNotNull(ctx);
    }

    @Test
    public void testNameServiceListing() throws Exception {
        String serverListing = cnc.getNameServiceListingFromServer();
        Assert.assertEquals(serverListing, NamingUtil.getNameServiceListing(rootContext));
    }

    @Test(expected = NO_PERMISSION.class)
    public void testNewContextFails() throws Exception {
        rootContext.new_context();
    }

    @Test(expected = NO_PERMISSION.class)
    public void testRebindFails() throws Exception {
        rootContext.rebind(NamingUtil.CNC_NAME, (org.omg.CORBA.Object) cnc);
    }

    @Test
    public void testResolvable() throws Exception {
        cnc.bindResolvable(NamingUtil.CNC_RESOLVABLE_NAME);
        Object o = rootContext.resolve(NamingUtil.CNC_RESOLVABLE_NAME);
        CosNamingChecker c = (CosNamingChecker) PortableRemoteObject.narrow(o, CosNamingChecker.class);
        assertNotNull(c);
    }

    @Test(expected = IllegalStateException.class)
    public void testResolvableThatThrows() throws Exception {
        cnc.bindResolvableThatThrows(new IllegalStateException("bob"), NamingUtil.CNC_RESOLVABLE_NAME);
        rootContext.resolve(NamingUtil.CNC_RESOLVABLE_NAME);
    }
}

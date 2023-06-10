/*
 * Copyright (c) 2020,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package test.corba.remote.war;

import static org.junit.Assert.assertNotNull;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;
import org.omg.CosNaming.NamingContextPackage.NotFound;

import componenttest.app.FATServlet;
import test.iiop.common.CosNamingChecker;
import test.iiop.common.NamingUtil;

@WebServlet("/CosNamingClientStringToObjectServlet")
@SuppressWarnings("serial")
public class CosNamingClientStringToObjectServlet extends FATServlet {
    @Resource
    private ORB orb;

    private NamingContext rootContext;
    private CosNamingChecker cnc;

    // retrieve the name service url from JNDI
    @Resource(lookup="name-service-url", type=String.class)
    private String nameServiceUrl;

    @PostConstruct
    public void setupUsingStringToObject() {
        rootContext = NamingContextHelper.narrow(orb.string_to_object(nameServiceUrl));
        try {
            cnc = (CosNamingChecker) PortableRemoteObject.narrow(rootContext.resolve(NamingUtil.CNC_NAME), CosNamingChecker.class);
        } catch (ClassCastException | NotFound | CannotProceed | InvalidName e) {
            throw new Error(e);
        }
    }

    @Test
    public void testNameServiceIsAvailableOnServer() throws Exception {
        cnc.checkNameServiceIsAvailable();
    }

    @Test
    public void testNameServiceIsAvailableRemotely() throws Exception {
        org.omg.CORBA.Object obj = orb.string_to_object(nameServiceUrl);
        NamingContextExt ctx = NamingContextExtHelper.narrow(obj);
        Assert.assertNotNull(ctx);
    }

    @Test
    public void testNameServiceListing() throws Exception {
        String serverListing = cnc.getNameServiceListingFromServer();
        Assert.assertEquals(serverListing, NamingUtil.getNameServiceListing(rootContext));
    }

    @Test
    public void testNewContextFails() throws Exception {
        try {
            rootContext.new_context();
            Assert.fail("Should have thrown NO_PERMISSION");
        } catch (NO_PERMISSION expected) {
        }
    }

    @Test
    public void testRebindFails() throws Exception {
        try {
            rootContext.rebind(NamingUtil.CNC_NAME, (org.omg.CORBA.Object) cnc);
            Assert.fail("Should have thrown NO_PERMISSION");
        } catch (NO_PERMISSION expected) {
        }
    }

    @Test
    public void testResolvable() throws Exception {
        cnc.bindResolvable(NamingUtil.CNC_RESOLVABLE_NAME);
        Object o = rootContext.resolve(NamingUtil.CNC_RESOLVABLE_NAME);
        CosNamingChecker c = (CosNamingChecker) PortableRemoteObject.narrow(o, CosNamingChecker.class);
        assertNotNull(c);
    }

    @Test
    public void testResolvableThatThrows() throws Exception {
        try {
            cnc.bindResolvableThatThrows(new IllegalStateException("bob"), NamingUtil.CNC_RESOLVABLE_NAME);
            rootContext.resolve(NamingUtil.CNC_RESOLVABLE_NAME);
            Assert.fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }
}

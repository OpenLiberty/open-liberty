/*
 * =============================================================================
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */

package servlet;

import componenttest.app.FATServlet;
import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.portable.InputStream;
import org.omg.CORBA.portable.OutputStream;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.IOP.IOR;
import org.omg.IOP.IORHelper;
import shared.TestRemote;

import javax.annotation.Resource;
import javax.rmi.CORBA.Stub;
import javax.rmi.PortableRemoteObject;
import javax.servlet.annotation.WebServlet;
import java.util.Objects;

@WebServlet("/IORTestServlet")
@SuppressWarnings("serial")
public class IORTestServlet extends FATServlet {
    @Resource
    private ORB orb;

    @Test
    public void testServletExists() {
        System.out.println("### It's alive! ###");
    }

    @Test
    public void testOrbInjected() {
        System.out.println( "### Examining orb reference: " + orb + " ###");
        Objects.requireNonNull(orb);
    }

    @Test
    public void testEjbLookup() throws Exception {
        Objects.requireNonNull(IiopLogic.lookupEjb(orb));
    }

    @Test
    public void testEjbIorHasButOneProfile() throws Exception {
        TestRemote ejb = IiopLogic.lookupEjb(orb);
        final int numProfiles = IiopLogic.getNumProfiles((Stub) ejb, orb);
        System.out.printf("### IOR retrieved, with %d profile(s).%n", numProfiles);
        Assert.assertEquals(numProfiles, 1);
    }

    /**
     * Place the IIOP-specific logic in here so the Servlet class can be safely introspected by the JUnit test.
     * (The JUnit test does not have all the Liberty and IIOP classes on its classpath.)
     */
    private enum IiopLogic {
        ;

        static TestRemote lookupEjb(ORB orb) throws Exception {
            System.out.println("### Performing naming lookup ###");
            NamingContext nameService = NamingContextHelper.narrow(orb.resolve_initial_references("NameService"));
            // ejb/global/TestCorba/TestCorbaEjb/TestEjb!shared.TestRemote
            NameComponent[] nameComponents = {
                    new NameComponent("ejb", ""),
                    new NameComponent("global", ""),
                    new NameComponent("TestCorba", ""),
                    new NameComponent("TestCorbaEjb", ""),
                    new NameComponent("TestEjb!shared.TestRemote", "")
            };
            Object o = nameService.resolve(nameComponents);
            TestRemote ejb = (TestRemote) PortableRemoteObject.narrow(o, TestRemote.class);
            System.out.println("### ejb = " + ejb + " ###");
            return ejb;
        }

        static int getNumProfiles(Stub ejb, ORB orb) {
            OutputStream os = orb.create_output_stream();
            os.write_Object(ejb);
            InputStream is = os.create_input_stream();
            IOR ior = IORHelper.read(is);
            return ior.profiles.length;
        }
    }
}


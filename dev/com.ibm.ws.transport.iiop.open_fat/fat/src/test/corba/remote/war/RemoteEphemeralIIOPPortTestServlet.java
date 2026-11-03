/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package test.corba.remote.war;

import java.util.Objects;

import javax.annotation.Resource;
import javax.rmi.CORBA.Stub;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;

import componenttest.app.FATServlet;
import shared.Business;
import shared.ClientUtil;
import shared.TestRemote;

/**
 * Client servlet that runs on a separate Liberty server (ephemeralIiopClient) and
 * invokes EJBs hosted on ephemeralIiopServer, which was started with iiopPort="-1".
 *
 * The client ORB is pre-configured via server.xml to point at the EJB server's
 * CosNaming service using the ephemeral port discovered at test startup:
 *
 *   &lt;orb nameService="corbaname::localhost:${iiop.ephemeral.port}"/&gt;
 *
 * The FAT test class (RemoteEphemeralIiopPortTest) starts the EJB server first,
 * reads the actual ephemeral port from the trace log, injects it as a bootstrap
 * variable, and then starts this client server.
 *
 * This proves that a true remote client (different JVM / different Liberty instance)
 * can reach an EJB published on an ephemeral IIOP port, provided it is told the port
 * out-of-band (here: via the FAT harness).
 */
@WebServlet("/RemoteEphemeralIiopPortTestServlet")
@SuppressWarnings("serial")
public class RemoteEphemeralIIOPPortTestServlet extends FATServlet {

    @Resource
    private ORB orb;

    /**
     * The injected ORB must not be null; if the nameService bootstrap variable
     * was resolved correctly Liberty will have initialised the ORB.
     */
    @Test
    public void testOrbInjected() {
        System.out.println("### Remote ephemeral-port test: orb=" + orb + " ###");
        Objects.requireNonNull(orb, "ORB must be injected");
    }

    /**
     * CosNaming lookup across JVMs using the ephemeral IIOP port.
     * The ORB's nameService points at corbaname::localhost:&lt;ephemeralPort&gt;,
     * so resolve_initial_references("NameService") issues a real TCP connection
     * to the EJB server.
     */
    @Test
    public void testRemoteEjbLookup() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        Objects.requireNonNull(bean, "TestBean must be found via remote CosNaming lookup");
    }

    /**
     * Full round-trip: look up the EJB over IIOP and invoke a method on it.
     * The EJB IOR returned by CosNaming carries the same ephemeral port, so
     * the method invocation also goes across the network to that port.
     */
    @Test
    public void testRemoteEjbInvocation() throws Exception {
        Business bean = ClientUtil.lookupBusinessBean(orb);
        Objects.requireNonNull(bean, "BusinessBean must be found via remote CosNaming lookup");
        Assert.assertEquals(
                "Remote invocation of BusinessBean.takesInt() across ephemeral IIOP port must return the same value",
                42, bean.takesInt(42));
    }

    /**
     * Verify the IOR the remote client receives contains exactly one plain
     * TAG_INTERNET_IOP profile (the ephemeral port) — no SSL profiles, no
     * zero-port profile — identical to the co-located case.
     */
    @Test
    public void testRemoteEjbIorProfiles() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        final int numProfiles = ClientUtil.getNumProfiles((Stub) bean, orb);
        System.out.printf("### Remote IOR profile count for ephemeral-port EJB server: %d%n", numProfiles);
        Assert.assertEquals(
                "IOR obtained by remote client must contain exactly one TAG_INTERNET_IOP profile",
                1, numProfiles);
    }
}

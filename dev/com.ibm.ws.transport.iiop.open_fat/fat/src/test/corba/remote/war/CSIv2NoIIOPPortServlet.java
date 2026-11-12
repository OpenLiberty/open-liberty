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

import java.util.Collections;
import java.util.Objects;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;
import javax.servlet.annotation.WebServlet;

import org.junit.Assert;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.IIOP.ProfileBody_1_1;

import componenttest.app.FATServlet;
import shared.Business;
import shared.ClientUtil;
import shared.TestRemote;

/**
 * Client servlet that runs on csiv2DisabledIIOPClient and invokes EJBs hosted on
 * csiv2NoIIOPPortServer, whose iiopEndpoint has no iiopPort attribute (defaults to -1)
 * and a single iiopsOptions child with iiopsPort only.
 *
 * The EJB server opens no plain IIOP socket at all, only one TLS/SSL socket on
 * iiopsPort. This servlet's ORB is pre-configured in server.xml to bootstrap
 * the CosNaming service over SSL:
 *
 *   &lt;orb nameService="corbaname:iiops:localhost:${bvt.prop.IIOP.secondary.secure}/NameService"/&gt;
 *
 * For the two-port variant see {@link CSIv2DisabledIIOPServlet}.
 */
@WebServlet("/CSIv2NoIIOPPortServlet")
@SuppressWarnings("serial")
public class CSIv2NoIIOPPortServlet extends FATServlet {

    /** The single iiopsPort the EJB server listens on (see csiv2NoIIOPPortServer/server.xml). */
    private static final String SSL_PORT = System.getProperty("bvt.prop.IIOP.secondary.secure");

    @Resource
    private ORB orb;

    /** The injected ORB must not be null; verifies the SSL client ORB initialised correctly. */
    @Test
    public void testOrbInjected() {
        System.out.println("### CSIv2 no-iiopPort client test: orb=" + orb + " ###");
        Objects.requireNonNull(orb, "ORB must be injected on SSL-only client");
    }

    /** Full round-trip over SSL: look up the EJB and invoke a method on it. */
    @Test
    public void testSslEjbInvocation() throws Exception {
        Business bean = ClientUtil.lookupBusinessBean(orb);
        Objects.requireNonNull(bean, "BusinessBean must be found via SSL CosNaming lookup");
        Assert.assertEquals(
                "Remote invocation of BusinessBean.takesInt() over SSL-only IIOP must return the same value",
                42, bean.takesInt(42));
    }

    /**
     * JNDI lookup over SSL using a full corbaname URL, without using the injected ORB,
     * followed by an invocation to prove the round-trip works over TLS.
     */
    @Test
    public void testSslEjbJndiLookup() throws Exception {
        String jndiName = "corbaname:iiops:localhost:" + SSL_PORT
                          + "#ejb/global/test.corba/test.corba.bean/BusinessBean!shared.Business";
        Object o = new InitialContext().lookup(jndiName);
        Business bean = (Business) PortableRemoteObject.narrow(o, Business.class);
        Objects.requireNonNull(bean, "BusinessBean must be found via JNDI corbaname:iiops lookup");
        Assert.assertEquals(
                "Remote invocation of BusinessBean.takesInt() after JNDI lookup over SSL-only IIOP must return the same value",
                42, bean.takesInt(42));
    }

    /** The IOR must contain exactly one IIOP profile, acting only as carrier for the CSIv2 component. */
    @Test
    public void testSslEjbIorProfiles() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        final int numProfiles = ClientUtil.getNumProfiles((Stub) bean, orb);
        System.out.printf("### CSIv2 no-iiopPort IOR profile count: %d%n", numProfiles);
        Assert.assertEquals(
                "IOR must contain exactly one IIOP profile when iiopPort is absent with CSIv2",
                1, numProfiles);
    }

    /**
     * The single IIOP profile must have plain port 0 and its CSIv2 TAG_CSI_SEC_MECH_LIST
     * component must advertise exactly one TLS transport address, the server's iiopsPort.
     */
    @Test
    public void testSslEjbIorAdvertisesOnlySslPort() throws Exception {
        final int expectedSslPort = Integer.parseInt(SSL_PORT);
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        ProfileBody_1_1 profile = ClientUtil.getIiopProfile((Stub) bean, orb);
        System.out.printf("### CSIv2 no-iiopPort IOR profile: host=%s port=%d%n", profile.host, profile.port & 0xFFFF);
        Assert.assertEquals("The plain IIOP port in the IOR profile must be 0 when iiopPort is absent",
                            0, profile.port & 0xFFFF);
        Assert.assertEquals("The CSIv2 component must advertise only the iiopsPort as TLS transport address",
                            Collections.singletonList(expectedSslPort), ClientUtil.getCsiv2SslPorts(profile, orb));
    }
}

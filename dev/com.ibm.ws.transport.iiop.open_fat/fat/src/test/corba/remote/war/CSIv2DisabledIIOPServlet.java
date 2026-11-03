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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
 * csiv2DisabledIIOPServer, which is configured with iiopPort="-1" and iiopsPort only.
 *
 * The EJB server opens no plain IIOP socket at all — only a TLS/SSL socket on
 * iiopsPort. This servlet's ORB is pre-configured in server.xml to bootstrap
 * the CosNaming service over SSL:
 *
 *   &lt;orb nameService="corbaname:iiops:localhost:${bvt.prop.IIOP.secondary.secure}/NameService"/&gt;
 *
 * This proves that a remote CSIv2-capable client can reach an EJB published on
 * an SSL-only server (iiopPort=-1).
 *
 */
@WebServlet("/CSIv2DisabledIIOPServlet")
@SuppressWarnings("serial")
public class CSIv2DisabledIIOPServlet extends FATServlet {

    /** The two iiopsPorts the EJB server listens on (see csiv2DisabledIIOPServer/server.xml). */
    private static final List<String> SSL_PORTS = Arrays.asList(System.getProperty("bvt.prop.IIOP.secure"),
                                                                System.getProperty("bvt.prop.IIOP.secondary.secure"));

    @Resource
    private ORB orb;

    /**
     * The injected ORB must not be null; verifies the SSL client ORB
     * initialised correctly and that appSecurity is active on the client.
     */
    @Test
    public void testOrbInjected() {
        System.out.println("### CSIv2 SSL-only client test: orb=" + orb + " ###");
        Objects.requireNonNull(orb, "ORB must be injected on SSL-only client");
    }
    
    /**
     * Full round-trip over SSL: look up the EJB and invoke a method on it.
     * The IOR returned by CosNaming carries the SSL transport address, so
     * the method invocation also travels over the TLS connection.
     * Proves end-to-end SSL-only IIOP works when iiopPort=-1.
     */
    @Test
    public void testSslEjbInvocation() throws Exception {
        Business bean = ClientUtil.lookupBusinessBean(orb);
        Objects.requireNonNull(bean, "BusinessBean must be found via SSL CosNaming lookup");
        Assert.assertEquals(
                "Remote invocation of BusinessBean.takesInt() over SSL-only IIOP must return the same value",
                42, bean.takesInt(42));
    }

    /**
     * JNDI lookup over SSL using a full corbaname URL, without using the injected ORB:
     *
     *   corbaname:iiops:localhost:&lt;iiopsPort&gt;#ejb/global/...
     *
     * The iiops protocol makes the client connect to the SSL-only port directly, so
     * no prior plain-IIOP lookup is needed. The resulting bean is then invoked to
     * prove the full round-trip works over TLS.
     */
    @Test
    public void testSslEjbJndiLookup() throws Exception {
        for (String sslPort : SSL_PORTS) {
            assertJndiLookupAndInvoke(sslPort);
        }
    }

    /** Looks up BusinessBean via corbaname:iiops on the given port and verifies a round-trip invocation. */
    private static void assertJndiLookupAndInvoke(String sslPort) throws Exception {
        String jndiName = "corbaname:iiops:localhost:" + sslPort
                          + "#ejb/global/test.corba/test.corba.bean/BusinessBean!shared.Business";
        Object o = new InitialContext().lookup(jndiName);
        Business bean = (Business) PortableRemoteObject.narrow(o, Business.class);
        Objects.requireNonNull(bean, "BusinessBean must be found via JNDI corbaname:iiops lookup on port " + sslPort);
        Assert.assertEquals(
                "Remote invocation of BusinessBean.takesInt() after JNDI lookup over SSL-only IIOP on port " + sslPort
                        + " must return the same value",
                42, bean.takesInt(42));
    }

    /**
     * Verify the IOR the SSL client receives contains exactly one IIOP profile.
     * With iiopPort=-1 no plain IIOP port is published, so the single profile
     * only acts as the carrier for the CSIv2 security component.
     */
    @Test
    public void testSslEjbIorProfiles() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        final int numProfiles = ClientUtil.getNumProfiles((Stub) bean, orb);
        System.out.printf("### CSIv2 SSL-only IOR profile count: %d%n", numProfiles);
        Assert.assertEquals(
                "IOR must contain exactly one IIOP profile when iiopPort=-1 with CSIv2",
                1, numProfiles);
    }

    /**
     * Verify the content of the single IIOP profile: its plain port must be 0 (no
     * plain IIOP endpoint is advertised) and the CSIv2 TAG_CSI_SEC_MECH_LIST component
     * must advertise exactly the two configured iiopsPorts as TLS transport addresses.
     */
    @Test
    public void testSslEjbIorAdvertisesOnlySslPort() throws Exception {
        final List<Integer> expectedSslPorts = new ArrayList<>();
        for (String sslPort : SSL_PORTS) {
            expectedSslPorts.add(Integer.parseInt(sslPort));
        }
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        ProfileBody_1_1 profile = ClientUtil.getIiopProfile((Stub) bean, orb);
        System.out.printf("### CSIv2 SSL-only IOR profile: host=%s port=%d%n", profile.host, profile.port & 0xFFFF);
        Assert.assertEquals("The plain IIOP port in the IOR profile must be 0 when iiopPort=-1",
                            0, profile.port & 0xFFFF);
        List<Integer> actualSslPorts = new ArrayList<>(ClientUtil.getCsiv2SslPorts(profile, orb));
        Collections.sort(expectedSslPorts);
        Collections.sort(actualSslPorts);
        Assert.assertEquals("The CSIv2 component must advertise exactly the two iiopsPorts as TLS transport addresses",
                            expectedSslPorts, actualSslPorts);
    }
}

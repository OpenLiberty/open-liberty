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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.rmi.CORBA.Stub;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.IIOP.ProfileBody_1_1;

import componenttest.app.FATServlet;
import shared.Business;
import shared.ClientUtil;
import shared.TestRemote;

/**
 * Client servlet that runs on csiv2BothPortsClient and uses full corbaname URLs to look up
 * EJBs hosted on csiv2BothPortsServer, which has BOTH a plain iiopPort and an SSL iiopsPort.
 *
 * <ul>
 * <li>{@code corbaname:iiop:localhost:<iiopPort>} must work (plain NameService lookup)</li>
 * <li>{@code corbaname:iiops:localhost:<iiopsPort>} must work (SSL NameService lookup)</li>
 * <li>{@code corbaname:iiop:localhost:<iiopsPort>} (plain protocol, SSL port) must not work</li>
 * <li>{@code corbaname:iiops:localhost:<iiopPort>} (SSL protocol, plain port) must not work</li>
 * </ul>
 *
 * The two cross lookups fail with {@code org.omg.CORBA.BAD_PARAM: error parsing URL: ...} in Yoko).
 *
 * <p>The servlet also injects the container ORB (whose nameService is pointed at the plain
 * iiopPort in server.xml) so it can perform IOR-level assertions: even with both ports open,
 * CSIv2 collapses the IOR to a single profile with {@code port=0}, and the real SSL transport
 * address is carried solely in the TAG_CSI_SEC_MECH_LIST component (iiopsPort).
 */
@WebServlet("/CSIv2BothPortsServlet")
@SuppressWarnings("serial")
public class CSIv2BothPortsServlet extends FATServlet {

    private static final String PLAIN_PORT = System.getProperty("bvt.prop.IIOP.secondary");
    private static final String SSL_PORT = System.getProperty("bvt.prop.IIOP.secondary.secure");

    /**
     * The container ORB configured in server.xml with
     * {@code nameService="corbaname:iiop:localhost:${bvt.prop.IIOP.secondary}/NameService"}
     * so resolve_initial_references("NameService") connects over plain IIOP.
     * Used only by the IOR-inspection tests below.
     */
    @Resource
    private ORB orb;

    private static final String BEAN_JNDI_NAME = "ejb/global/test.corba/test.corba.bean/BusinessBean!shared.Business";

    private static String corbaname(String protocol, String port, boolean includeNameService) {
        return "corbaname:" + protocol + ":localhost:" + port + (includeNameService ? "/NameService" : "") + "#" + BEAN_JNDI_NAME;
    }

    private static Business lookupBusinessBean(String jndiName) throws Exception {
        System.out.println("### Looking up " + jndiName + " ###");
        Object o = new InitialContext().lookup(jndiName);
        Business bean = (Business) PortableRemoteObject.narrow(o, Business.class);
        assertNotNull("BusinessBean must be found via " + jndiName, bean);
        return bean;
    }

    private static void assertCrossLookupFails(String jndiName) throws Exception {
        try {
            Business bean = lookupBusinessBean(jndiName);
            fail("Cross-protocol lookup must not succeed: " + jndiName + " returned " + bean);
        } catch (Exception expected) {
            System.out.println("### Expected exception for " + jndiName + ": " + expected + " ###");
            assertTrue("Expected an 'error parsing URL' BAD_PARAM but got: " + expected,
                       String.valueOf(expected.getMessage()).contains("error parsing URL"));
        }
    }

    // -----------------------------------------------------------------------
    // Connectivity tests (corbaname URL lookups)
    // -----------------------------------------------------------------------

    /** Plain lookup of the NameService on the plain iiopPort succeeds and the bean can be invoked. */
    @Test
    public void testPlainLookupViaIiopPort() throws Exception {
        Business bean = lookupBusinessBean(corbaname("iiop", PLAIN_PORT, true));
        assertEquals("Remote invocation of BusinessBean.takesInt() after corbaname:iiop lookup must return the same value",
                     42, bean.takesInt(42));
    }

    /** SSL lookup of the NameService on the SSL iiopsPort succeeds and the bean can be invoked. */
    @Test
    public void testSslLookupViaIiopsPort() throws Exception {
        Business bean = lookupBusinessBean(corbaname("iiops", SSL_PORT, true));
        assertEquals("Remote invocation of BusinessBean.takesInt() after corbaname:iiops lookup must return the same value",
                     42, bean.takesInt(42));
    }

    /** Both protocols can be used one after the other from the same client (no cross-protocol connection reuse). */
    @Test
    public void testPlainThenSslLookup() throws Exception {
        assertEquals(1, lookupBusinessBean(corbaname("iiop", PLAIN_PORT, true)).takesInt(1));
        assertEquals(2, lookupBusinessBean(corbaname("iiops", SSL_PORT, true)).takesInt(2));
        assertEquals(3, lookupBusinessBean(corbaname("iiop", PLAIN_PORT, true)).takesInt(3));
    }

    /** A plain (iiop) URL pointing at the SSL (iiops) port must fail. */
    @Test
    public void testPlainProtocolOnIiopsPortFails() throws Exception {
        assertCrossLookupFails(corbaname("iiop", SSL_PORT, false));
    }

    /** An SSL (iiops) URL pointing at the plain (iiop) port must fail. */
    @Test
    public void testSslProtocolOnIiopPortFails() throws Exception {
        assertCrossLookupFails(corbaname("iiops", PLAIN_PORT, false));
    }

    /** The cross lookups must also fail when the NameService is named explicitly. */
    @Test
    public void testCrossLookupsWithExplicitNameServiceFail() throws Exception {
        assertCrossLookupFails(corbaname("iiop", SSL_PORT, true));
        assertCrossLookupFails(corbaname("iiops", PLAIN_PORT, true));
    }

    /** A failed cross lookup must not poison later valid lookups. */
    @Test
    public void testValidLookupsStillWorkAfterCrossLookupFailures() throws Exception {
        assertCrossLookupFails(corbaname("iiop", SSL_PORT, false));
        assertCrossLookupFails(corbaname("iiops", PLAIN_PORT, false));
        assertEquals(7, lookupBusinessBean(corbaname("iiop", PLAIN_PORT, true)).takesInt(7));
        assertEquals(8, lookupBusinessBean(corbaname("iiops", SSL_PORT, true)).takesInt(8));
    }

    // -----------------------------------------------------------------------
    // IOR-structure tests
    // -----------------------------------------------------------------------

    /**
     * When both iiopPort and iiopsPort are open but CSIv2 is active, Yoko still publishes
     * exactly one TAG_INTERNET_IOP profile.  CSIv2 collapses the IOR to a single profile
     * with {@code port=0} — the real transport address is carried in the
     * TAG_CSI_SEC_MECH_LIST component, not in the profile body port field.
     */
    @Test
    public void testIorContainsSingleProfile() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        final int numProfiles = ClientUtil.getNumProfiles((Stub) bean, orb);
        System.out.printf("### CSIv2 both-ports IOR profile count: %d%n", numProfiles);
        assertEquals(
                "IOR must contain exactly one IIOP profile when CSIv2 is active (port=0 convention)",
                1, numProfiles);
    }

    /**
     * With CSIv2 active the IIOP profile body port field must be 0, regardless of whether
     * iiopPort is open.  Port=0 is the OMG-defined signal that clients must use the
     * TAG_CSI_SEC_MECH_LIST component to find the real transport address.
     */
    @Test
    public void testIorProfilePortIsZeroWithCsiv2() throws Exception {
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        ProfileBody_1_1 profile = ClientUtil.getIiopProfile((Stub) bean, orb);
        System.out.printf("### CSIv2 both-ports IOR profile: host=%s port=%d%n",
                          profile.host, profile.port & 0xFFFF);
        assertEquals(
                "The IIOP profile body port must be 0 when CSIv2 is active (clients use the CSIv2 component)",
                0, profile.port & 0xFFFF);
    }

    /**
     * The CSIv2 TAG_CSI_SEC_MECH_LIST component embedded in the IOR profile must advertise
     * the SSL iiopsPort as its sole TLS transport address.  When both ports are open the
     * server publishes exactly one TLS transport address: the iiopsPort.
     */
    @Test
    public void testIorCsiv2ComponentAdvertisesSslPort() throws Exception {
        final int expectedSslPort = Integer.parseInt(SSL_PORT);
        TestRemote bean = ClientUtil.lookupTestBean(orb);
        ProfileBody_1_1 profile = ClientUtil.getIiopProfile((Stub) bean, orb);
        List<Integer> sslPorts = ClientUtil.getCsiv2SslPorts(profile, orb);
        System.out.printf("### CSIv2 both-ports IOR CSIv2 SSL ports: %s%n", sslPorts);
        assertEquals(
                "The CSIv2 component must advertise exactly the iiopsPort as TLS transport address",
                Arrays.asList(expectedSslPort), sslPorts);
    }
}

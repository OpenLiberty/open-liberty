/*
 * Copyright (c) 2014,2023 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.server.config.tss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.Integrity;
import org.omg.CSIIOP.TransportAddress;

import test.common.SharedOutputManager;

import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSSLTransportConfig;

public class ServerConfigHelperTest {

    /**  */
    private static final String SUPPORTED_CLIENT_SSL_CONFIG = "supportedClientSSLConfig";
    private static final String REQUIRED_CLIENT_SSL_CONFIG = "requiredClientSSLConfig";
    private static final String SSL_CONFIG_NOT_IN_IIOPS_OPTIONS = "sslConfigNotInIIOPSOptions";
    private static final String DEFAULT_ALIAS = "testDefaultAlias";
    private static final Collection<String> KNOWN_ALIASES = Arrays.asList(new String[] { SUPPORTED_CLIENT_SSL_CONFIG, REQUIRED_CLIENT_SSL_CONFIG, DEFAULT_ALIAS,
                                                                                        SSL_CONFIG_NOT_IN_IIOPS_OPTIONS });

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final String[] LTPA = { "LTPA" };
    private static final String[] GSSUP = { "GSSUP" };
    private static final String[] GSSUP_LTPA = { "GSSUP", "LTPA" };
    private static final String[] LTPA_GSSUP = { "LTPA", "GSSUP" };
    private static final String[] GSSUP_GSSUP = { "gssup", "GSSUP" };
    private static final String[] LTPA_LTPA = { "ltpa", "LTPA" };
    private static final String[] INVALID_AND_GSSUP_MECH = { "INVALID", "GSSUP" };
    private static final String[] EMPTY_MECH = { "" };

    private ServerConfigHelper serverConfigHelper;
    private Authenticator authenticator;
    private TokenManager tokenManager;
    private UnauthenticatedSubjectService unauthenticatedSubjectService;
    private String targetName;
    private Map<String, Object> properties;
    private Map<String, List<TransportAddress>> addrMap;

    @Before
    public void setUp() {
        SecurityServices.setupSSLConfig(new SSLConfig(null) {

            /*
             * (non-Javadoc)
             *
             * @see com.ibm.ws.security.csiv2.config.ssl.SSLConfig#getAssociationOptions(java.lang.String)
             */
            @Override
            public OptionsKey getAssociationOptions(String sslAliasName) throws SSLException {
                if (!KNOWN_ALIASES.contains(sslAliasName))
                    throw new IllegalArgumentException("Unknown alias: " + sslAliasName);
                short clientAuthRequired = sslAliasName.contains("required") ? EstablishTrustInClient.value : 0;
                short clientAuthSupported = sslAliasName.contains("supported") ? EstablishTrustInClient.value : clientAuthRequired;
                return new OptionsKey((short) (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value | clientAuthSupported),
                                (short) (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value | clientAuthRequired));
            }

        });
        serverConfigHelper = new ServerConfigHelper(authenticator, tokenManager, unauthenticatedSubjectService, targetName, DEFAULT_ALIAS);
        properties = createDefaultInstances();
        addConfigFromServerXMLWithEmptyLayersElement(properties);
        addrMap = createAddressMap();
    }

    private Map<String, Object> createDefaultInstances() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("serverPolicy.0.config.referenceType", "com.ibm.ws.security.csiv2.serverPolicyCSIV2");
        properties.put("serverPolicy.0.stateful", false);
        // Layers
        properties.put("serverPolicy.0.layers.0.config.referenceType", "com.ibm.ws.security.csiv2.serverPolicyLayers");

        properties.put("serverPolicy.0.layers.0.attributeLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.attributeLayer");
        properties.put("serverPolicy.0.layers.0.attributeLayer.0.identityAssertionEnabled", false);

        properties.put("serverPolicy.0.layers.0.authenticationLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.authenticationLayer");
        properties.put("serverPolicy.0.layers.0.authenticationLayer.0.mechanisms", LTPA_GSSUP);
        properties.put("serverPolicy.0.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        properties.put("serverPolicy.0.layers.0.transportLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.transportLayer");
        properties.put("serverPolicy.0.layers.0.transportLayer.0.sslEnabled", true);
//        properties.put("serverPolicy.0.layers.0.transportLayer.0.sslRef", DEFAULT_ALIAS);

        return properties;
    }

    private void addConfigFromServerXMLWithEmptyLayersElement(Map<String, Object> properties) {
        // CSIv2
        properties.put("serverPolicy.1.config.referenceType", "com.ibm.ws.security.csiv2.serverPolicyCSIV2");
        properties.put("serverPolicy.1.stateful", false);
        // Layers
        properties.put("serverPolicy.1.layers.0.config.referenceType", "com.ibm.ws.security.csiv2.serverPolicyLayers");
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @Test
    public void testEmptyLayers() throws Exception {
        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        assertNotNull("There must be a TSSConfig object.", tssConfig);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();

        assertDefaultConfig(compoundSecMechList);
    }

    @Test
    public void testNoLayersElement() throws Exception {
        properties = createDefaultInstances();
        properties.put("serverPolicy.1.config.referenceType", "com.ibm.ws.security.csiv2.serverPolicyCSIV2");
        properties.put("serverPolicy.1.stateful", false);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();

        assertDefaultConfig(compoundSecMechList);
    }

    @Test
    public void testDefaultInstances() throws Exception {
        properties = createDefaultInstances();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();

        assertDefaultConfig(compoundSecMechList);
    }

    private void assertDefaultConfig(TSSCompoundSecMechListConfig compoundSecMechList) {
        assertEquals("There must be two compound sec mechs in the list.", 2, compoundSecMechList.size());

        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        assertDefaultLTPA(compoundSecMechConfig);

        compoundSecMechConfig = compoundSecMechList.mechAt(1);
        assertDefaultGSSUP(compoundSecMechConfig);
    }

    private void assertDefaultLTPA(TSSCompoundSecMechConfig compoundSecMechConfig) {
        assertTrue("The transport layer must be enabled with type TSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSSSLTransportConfig);
        assertTrue("The authentication layer must be enabled with type ServerLTPAMechConfig.", compoundSecMechConfig.getAs_mech() instanceof ServerLTPAMechConfig);
        assertEquals("The attribute layer must not support identity assertion.", 0, compoundSecMechConfig.getSas_mech().getSupports());
    }

    private void assertDefaultGSSUP(TSSCompoundSecMechConfig compoundSecMechConfig) {
        assertTrue("The transport layer must be enabled with type TSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSSSLTransportConfig);
        assertTrue("The authentication layer must be enabled with type TSSGSSUPMechConfig.", compoundSecMechConfig.getAs_mech() instanceof TSSGSSUPMechConfig);
        assertEquals("The attribute layer must not support identity assertion.", 0, compoundSecMechConfig.getSas_mech().getSupports());
    }

    @Test
    public void testTransportLayerDefault() throws Exception {
        putTransportLayerProperties();
        addrMap = createAddressMap();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertTrue("The transport layer must be enabled with type TSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSSSLTransportConfig);
        assertEquals(0, compoundSecMechConfig.getTransport_mech().getSupports() & EstablishTrustInClient.value);
        assertEquals(0, compoundSecMechConfig.getTransport_mech().getRequires() & EstablishTrustInClient.value);
    }

    @Test
    public void testTransportLayerSupports() throws Exception {
        putTransportLayerProperties();
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslRef", SUPPORTED_CLIENT_SSL_CONFIG);
        addrMap = createAddressMap();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertTrue("The transport layer must be enabled with type TSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSSSLTransportConfig);
        assertEquals(EstablishTrustInClient.value, compoundSecMechConfig.getTransport_mech().getSupports() & EstablishTrustInClient.value);
        assertEquals(0, compoundSecMechConfig.getTransport_mech().getRequires() & EstablishTrustInClient.value);
    }

    @Test
    public void testTransportLayerRequires() throws Exception {
        putTransportLayerProperties();
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslRef", REQUIRED_CLIENT_SSL_CONFIG);
        addrMap = createAddressMap();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertTrue("The transport layer must be enabled with type TSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSSSLTransportConfig);
        assertEquals(EstablishTrustInClient.value, compoundSecMechConfig.getTransport_mech().getSupports() & EstablishTrustInClient.value);
        assertEquals(EstablishTrustInClient.value, compoundSecMechConfig.getTransport_mech().getRequires() & EstablishTrustInClient.value);
    }

    @Test
    public void testTransportLayerSSLDisabled() throws Exception {
        putTransportLayerProperties();
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslEnabled", false);
        addrMap = createAddressMap();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertTrue("The transport layer must be enabled with type TSSNULLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof TSSNULLTransportConfig);
    }

    @Test
    public void testTransportLayerIllegalStateException() throws Exception {
        putTransportLayerProperties();
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslRef", SSL_CONFIG_NOT_IN_IIOPS_OPTIONS);
        addrMap = createAddressMap();

        try {
            serverConfigHelper.getTSSConfig(properties, addrMap);
            fail("There must be an IllegalStateException thrown.");
        } catch (IllegalStateException e) {
            assertTrue("The error message must be set in the exception.", e.getMessage().startsWith("CWWKS9622E"));
        }
    }

    @Test
    public void testTransportLayerIllegalStateExceptionForNoSecuredAddresses() throws Exception {
        putTransportLayerProperties();
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslRef", SSL_CONFIG_NOT_IN_IIOPS_OPTIONS);

        Map<String, List<TransportAddress>> addrMap = new HashMap<String, List<TransportAddress>>();
        List<TransportAddress> transportAddresses = new ArrayList<TransportAddress>();
        TransportAddress transportAddress = new TransportAddress();
        transportAddresses.add(transportAddress);
        addrMap.put(null, transportAddresses); // This is how unsecured addresses are added

        try {
            serverConfigHelper.getTSSConfig(properties, addrMap);
            fail("There must be an IllegalStateException thrown.");
        } catch (IllegalStateException e) {
            assertTrue("The error message must be set in the exception.", e.getMessage().startsWith("CWWKS9623E"));
        }
    }

    private void putTransportLayerProperties() {
        properties.put("serverPolicy.1.layers.0.transportLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.transportLayer");
        properties.put("serverPolicy.1.layers.0.transportLayer.0.sslEnabled", true);
    }

    private Map<String, List<TransportAddress>> createAddressMap() {
        Map<String, List<TransportAddress>> addrMap = new HashMap<String, List<TransportAddress>>();
        List<TransportAddress> transportAddresses = new ArrayList<TransportAddress>();
        TransportAddress transportAddress = new TransportAddress();
        transportAddresses.add(transportAddress);
        addrMap.put(DEFAULT_ALIAS, transportAddresses);
        addrMap.put(SUPPORTED_CLIENT_SSL_CONFIG, transportAddresses);
        addrMap.put(REQUIRED_CLIENT_SSL_CONFIG, transportAddresses);
        return addrMap;
    }

    @Test
    public void testAuthenticationLayerLTPA() throws Exception {
        putAuthLayerProperties(LTPA);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());
        assertDefaultLTPA(compoundSecMechConfig);
    }

    @Test
    public void testAuthenticationLayerGSSUP() throws Exception {
        putAuthLayerProperties(GSSUP);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());
        assertDefaultGSSUP(compoundSecMechConfig);
    }

    @Test
    public void testAuthenticationLayerGSSUP_LTPA() throws Exception {
        putAuthLayerProperties(GSSUP_LTPA);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();

        assertEquals("There must be two compound sec mechs in the list.", 2, compoundSecMechList.size());

        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        assertDefaultGSSUP(compoundSecMechConfig);

        compoundSecMechConfig = compoundSecMechList.mechAt(1);
        assertDefaultLTPA(compoundSecMechConfig);
    }

    @Test
    public void testAuthenticationLayerLTPA_GSSUP() throws Exception {
        putAuthLayerProperties(LTPA_GSSUP);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();

        assertEquals("There must be two compound sec mechs in the list.", 2, compoundSecMechList.size());

        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        assertDefaultLTPA(compoundSecMechConfig);

        compoundSecMechConfig = compoundSecMechList.mechAt(1);
        assertDefaultGSSUP(compoundSecMechConfig);
    }

    @Test
    public void testAuthenticationLayerLTPA_required() throws Exception {
        putAuthLayerProperties(LTPA);
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The authentication layer authentication mechanism must be required.", EstablishTrustInClient.value, compoundSecMechConfig.getAs_mech().getRequires());
    }

    @Test
    public void testAuthenticationLayerGSSUP_required() throws Exception {
        putAuthLayerProperties(GSSUP);
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The authentication layer authentication mechanism must be required.", EstablishTrustInClient.value, compoundSecMechConfig.getAs_mech().getRequires());
    }

    @Test
    public void testAuthenticationLayer_never() throws Exception {
        putAuthLayerProperties(LTPA);
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Never");

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());
        assertTrue("The authentication layer must be disabled.", compoundSecMechConfig.getAs_mech() instanceof TSSNULLASMechConfig);
    }

    @Test
    public void testAuthenticationLayer_emptyMechanism() throws Exception {
        putAuthLayerProperties(EMPTY_MECH);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());
        assertTrue("The authentication layer must be disabled.", compoundSecMechConfig.getAs_mech() instanceof TSSNULLASMechConfig);
    }

    @Test
    public void testAuthenticationLayer_unknownMechanism() throws Exception {
        putAuthLayerProperties(INVALID_AND_GSSUP_MECH);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());
        assertTrue("The authentication layer must be enabled with type TSSGSSUPMechConfig.", compoundSecMechConfig.getAs_mech() instanceof TSSGSSUPMechConfig);

        assertTrue("Expected message was not logged", outputMgr.checkForMessages("CWWKS9620E:"));
    }

    private void putAuthLayerProperties(String[] mechanism) {
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.authenticationLayer");
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.mechanisms", mechanism);
        properties.put("serverPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Supported");
    }

    @Test
    public void testAttributeLayer() throws Exception {
        putAttributeLayerProperties();
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", true);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The attribute layer must support identity assertion.",
                     IdentityAssertion.value, compoundSecMechConfig.getSas_mech().getSupports());
    }

    @Test
    public void testAttributeLayer_disabled() throws Exception {
        putAttributeLayerProperties();

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The attribute layer must not support identity assertion.", 0, compoundSecMechConfig.getSas_mech().getSupports());
    }

    @Test
    public void testAttributeLayer_supportedIdentitiesTypes() throws Exception {
        putAttributeLayerProperties();
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", true);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The attribute layer must support identity assertion with Anonymous, PrincipalName, X509CertChain, and DistinguishedName.",
                     ITTAnonymous.value | ITTPrincipalName.value | ITTX509CertChain.value | ITTDistinguishedName.value,
                     compoundSecMechConfig.getSas_mech().getSupportedIdentityTypes());
    }

    @Test
    public void testAttributeLayer_supportedIdentitiesTypesOnlyITTDistinguishedName() throws Exception {
        putAttributeLayerProperties();
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionTypes", new String[] { "ITTDistinguishedName" });
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", true);

        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);
        TSSCompoundSecMechListConfig compoundSecMechList = tssConfig.getMechListConfig();
        TSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertEquals("The attribute layer must support identity assertion with DistinguishedName.", ITTDistinguishedName.value,
                     compoundSecMechConfig.getSas_mech().getSupportedIdentityTypes());
    }

    /**
     * Test authentication layer configured with mechanism: GSSUP and gssup
     * This should create only one element list of TSSCompoundSecMechConfig object
     * Verifying the only one element is created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerLowerAndUpperCaseMechanism_GSSUP() throws Exception {
        putAuthLayerProperties(GSSUP_GSSUP);
        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);

        /* Validate Mechanism: GSSUP. */
        assertEquals("The authentication layer must be enabled with type TSSGSSUPMechConfig.",
                     1, tssConfig.getMechListConfig().size());
    }

    /**
     * Test authentication layer configured with mechanism: LTPA and ltpa
     * This should create only one element list of TSSCompoundSecMechConfig object
     * Verifying the only one element is created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerLowerAndUpperCaseMechanism_LTPA() throws Exception {
        putAuthLayerProperties(LTPA_LTPA);
        TSSConfig tssConfig = serverConfigHelper.getTSSConfig(properties, addrMap);

        /* Validate Mechanism: LTPA. */
        assertEquals("The authentication layer must be enabled with type ServerLTPAMechConfig.",
                     1, tssConfig.getMechListConfig().size());
    }

    private void putAttributeLayerProperties() {
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.attributeLayer");
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", false);
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.identityAssertionTypes", new String[] { "ITTAnonymous", "ITTPrincipalName", "ITTX509CertChain",
                                                                                                        "ITTDistinguishedName" });
        properties.put("serverPolicy.1.layers.0.attributeLayer.0.trustedIdentities", "trustedId");
    }

}

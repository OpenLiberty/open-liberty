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
package com.ibm.ws.security.csiv2.server.config.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSIIOP.Confidentiality;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.CSIIOP.EstablishTrustInTarget;
import org.omg.CSIIOP.IdentityAssertion;
import org.omg.CSIIOP.Integrity;

import com.ibm.websphere.ssl.Constants;
import com.ibm.websphere.ssl.JSSEHelper;
import com.ibm.websphere.ssl.SSLException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.config.ssl.SSLConfig;
import com.ibm.ws.security.csiv2.util.SecurityServices;
import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSCompoundSecMechListConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic;
import com.ibm.ws.transport.iiop.security.config.css.CSSNULLASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSNULLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.css.CSSSSLTransportConfig;
import com.ibm.ws.transport.iiop.security.config.tss.OptionsKey;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

import test.common.SharedOutputManager;

public class ClientConfigHelperTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private static final String SUPPORTED_CLIENT_SSL_CONFIG = "supportedClientSSLConfig";
    private static final String REQUIRED_CLIENT_SSL_CONFIG = "requiredClientSSLConfig";
    private static final String DEFAULT_ALIAS = "testDefaultAlias";
    private static final Collection<String> KNOWN_ALIASES = Arrays.asList(new String[] { SUPPORTED_CLIENT_SSL_CONFIG, REQUIRED_CLIENT_SSL_CONFIG, DEFAULT_ALIAS });

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    @Rule
    public TestRule managerRule = outputMgr;

    private static final String[] LTPA = { "LTPA" };
    private static final String[] GSSUP = { "GSSUP" };
    private static final String[] LTPA_GSSUP = { "LTPA", "GSSUP" };
    private static final String[] GSSUP_GSSUP = { "gssup", "GSSUP" };
    private static final String[] LTPA_LTPA = { "ltpa", "LTPA" };
    private static final String[] INVALID_AND_LTPA_MECH = { "INVALID", "LTPA" };
    private static final String[] EMPTY_MECH = { "" };
    private static final String TRUSTED_ID = "trustedId";
    private static final String TRUSTED_PWD = "trustedIdPwd";
    private ClientConfigHelper clientConfigHelper;
    private final Authenticator authenticator = null;
    private final String domain = "testRealm";
    private Map<String, Object> properties;

    private final JSSEHelper jsseHelper = mockery.mock(JSSEHelper.class);

    @Before
    public void setUp() throws Exception {
        properties = createDefaultInstances();
        addConfigFromServerXMLWithEmptyLayersElement(properties);

        mockery.checking(new Expectations() {
            {
                Map<String, Object> connectionInfo = new HashMap<String, Object>();
                connectionInfo.put(Constants.CONNECTION_INFO_DIRECTION, Constants.DIRECTION_OUTBOUND);

                Properties props = new Properties();
                props.put("com.ibm.ssl.alias", DEFAULT_ALIAS);
                allowing(jsseHelper).getProperties(null, connectionInfo, null);
                will(returnValue(props));
            }
        });

        SecurityServices.setupSSLConfig(new SSLConfig(jsseHelper) {

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
                return new OptionsKey((short) (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value
                                               | clientAuthSupported), (short) (Integrity.value | Confidentiality.value | EstablishTrustInTarget.value | clientAuthRequired));
            }

        });
        clientConfigHelper = new ClientConfigHelper(authenticator, domain, DEFAULT_ALIAS);

    }

    private Map<String, Object> createDefaultInstances() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("clientPolicy.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyCSIV2");
        properties.put("clientPolicy.0.stateful", false);
        // Layers
        properties.put("clientPolicy.0.layers.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyLayers");

        properties.put("clientPolicy.0.layers.0.attributeLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyAttributeLayer");
        properties.put("clientPolicy.0.layers.0.attributeLayer.0.identityAssertionEnabled", false);

        properties.put("clientPolicy.0.layers.0.authenticationLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyAuthenticationLayer");
        properties.put("clientPolicy.0.layers.0.authenticationLayer.0.mechanisms", LTPA_GSSUP);
        properties.put("clientPolicy.0.layers.0.authenticationLayer.0.establishTrustInClient", "Supported");

        properties.put("clientPolicy.0.layers.0.transportLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.transportLayer");
        properties.put("clientPolicy.0.layers.0.transportLayer.0.sslEnabled", true);
//        properties.put("clientPolicy.0.layers.0.transportLayer.0.sslRef", DEFAULT_ALIAS);

        return properties;
    }

    private void addConfigFromServerXMLWithEmptyLayersElement(Map<String, Object> properties) {
        // CSIv2
        properties.put("clientPolicy.1.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyCSIV2");
        properties.put("clientPolicy.1.stateful", false);
        // Layers
        properties.put("clientPolicy.1.layers.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyLayers");
    }

    @After
    public void tearDown() {
        outputMgr.resetStreams();
    }

    @Test
    public void testEmptyLayers() throws Exception {
        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        assertNotNull("There must be a CSSConfig object.", cssConfig);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertDefaultConfig(compoundSecMechList);
    }

    @Test
    public void testNoLayersElement() throws Exception {
        properties = createDefaultInstances();
        properties.put("clientPolicy.1.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyCSIV2");
        properties.put("clientPolicy.1.stateful", false);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertDefaultConfig(compoundSecMechList);
    }

    @Test
    public void testDefaultInstances() throws Exception {
        properties = createDefaultInstances();

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertDefaultConfig(compoundSecMechList);
    }

    private void assertDefaultConfig(CSSCompoundSecMechListConfig compoundSecMechList) {
        assertEquals("There must be two compound sec mechs in the list.", 2, compoundSecMechList.size());

        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        assertDefaultLTPA(compoundSecMechConfig);

        compoundSecMechConfig = compoundSecMechList.mechAt(1);
        assertDefaultGSSUP(compoundSecMechConfig);
    }

    private void assertDefaultLTPA(CSSCompoundSecMechConfig compoundSecMechConfig) {
        assertTrue("The transport layer must be enabled with type CSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof CSSSSLTransportConfig);
        assertTrue("The authentication layer must be enabled with type ClientLTPAMechConfig.", compoundSecMechConfig.getAs_mech() instanceof ClientLTPAMechConfig);
        assertEquals("The attribute layer must not support identity assertion.", 0, compoundSecMechConfig.getSas_mech().getSupports());
    }

    private void assertDefaultGSSUP(CSSCompoundSecMechConfig compoundSecMechConfig) {
        assertTrue("The transport layer must be enabled with type CSSSSLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof CSSSSLTransportConfig);
        assertTrue("The authentication layer must be enabled with type CSSGSSUPMechConfigDynamic.", compoundSecMechConfig.getAs_mech() instanceof CSSGSSUPMechConfigDynamic);
        assertEquals("The attribute layer must not support identity assertion.", 0, compoundSecMechConfig.getSas_mech().getSupports());
    }

    /**
     * Test transport layer configured with EstablishTrustInClient: Required
     * Verifying required value is set to EstablishTrustInClient.value
     *
     * @throws Exception
     */
    @Test
    public void transportRequire() throws Exception {
        putTransportLayerProperties();
        properties.put("clientPolicy.1.layers.0.transportLayer.0.sslRef", REQUIRED_CLIENT_SSL_CONFIG);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate stateful: false */
        assertFalse("The stateful flag must be set to false.", cssConfig.getMechList().isStateful());
        /* Validate EstablishTrustInClient: required. */
        assertEquals("The transport layer must require EstablishTrustInClient.",
                     EstablishTrustInClient.value, cssConfig.getMechList().mechAt(0).getTransport_mech().getRequires() & EstablishTrustInClient.value);
        assertEquals("The transport layer must support EstablishTrustInClient.",
                     EstablishTrustInClient.value, cssConfig.getMechList().mechAt(0).getTransport_mech().getSupports() & EstablishTrustInClient.value);
    }

    /**
     * Test transport layer configured with EstablishTrustInClient: Supported
     * Verifying supported value is set to EstablishTrustInClient.value
     *
     * @throws Exception
     */
    @Test
    public void transportSupported() throws Exception {
        putTransportLayerProperties();
        properties.put("clientPolicy.1.layers.0.transportLayer.0.sslRef", SUPPORTED_CLIENT_SSL_CONFIG);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate EstablishTrustInClient: supported. */
        assertEquals("The transport layer must support EstablishTrustInClient.",
                     EstablishTrustInClient.value, cssConfig.getMechList().mechAt(0).getTransport_mech().getSupports() & EstablishTrustInClient.value);
        assertEquals("The transport layer must not support EstablishTrustInClient.",
                     0, cssConfig.getMechList().mechAt(0).getTransport_mech().getRequires() & EstablishTrustInClient.value);
    }

    @Test
    public void transportDefaultAlias() throws Exception {
        putTransportLayerProperties();

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate EstablishTrustInClient: not supported. */
        assertEquals("The transport layer must not require EstablishTrustInClient.",
                     0, cssConfig.getMechList().mechAt(0).getTransport_mech().getSupports() & EstablishTrustInClient.value);
        assertEquals("The transport layer must not support EstablishTrustInClient.",
                     0, cssConfig.getMechList().mechAt(0).getTransport_mech().getRequires() & EstablishTrustInClient.value);
    }

    @Test
    public void testTransportLayerSSLDisabled() throws Exception {
        putTransportLayerProperties();
        properties.put("clientPolicy.1.layers.0.transportLayer.0.sslEnabled", false);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();
        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);

        assertTrue("The transport layer must be enabled with type CSSNULLTransportConfig.", compoundSecMechConfig.getTransport_mech() instanceof CSSNULLTransportConfig);
    }

    private void putTransportLayerProperties() {
        properties.put("clientPolicy.1.layers.0.transportLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.transportLayer");
        properties.put("clientPolicy.1.layers.0.transportLayer.0.sslEnabled", true);
    }

    /**
     * Test authentication layer configured with EstablishTrustInClient: Required and mechanism: GSSUP and LTPA
     * Verifying required value is set to EstablishTrustInClient.value
     * Verifying CSSGSSUPMechConfigDynamic object created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerGSSUP() throws Exception {
        putAuthLayerProperties(GSSUP);
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());

        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        /* Validate EstablishTrustInClient: required. */
        assertEquals("The authentication layer must require EstablishTrustInClient.",
                     EstablishTrustInClient.value, compoundSecMechConfig.getAs_mech().getRequires());
        /* Validate authentication mechanism is instance of CSSGSSUPMechConfigDynamic */
        assertTrue("The authentication layer must be enabled with type CSSGSSUPMechConfigDynamic.",
                   compoundSecMechConfig.getAs_mech() instanceof CSSGSSUPMechConfigDynamic);
    }

    /**
     * Test authentication layer configured with EstablishTrustInClient: Required and mechanism: GSSUP and LTPA
     * Verifying required value is set to EstablishTrustInClient.value
     * Verifying ClientLTPAMechConfig object created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerLTPA() throws Exception {
        putAuthLayerProperties(LTPA);
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertEquals("There must be only one compound sec mech in the list.", 1, compoundSecMechList.size());

        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        /* Validate EstablishTrustInClient: required. */
        assertEquals("The authentication layer must require EstablishTrustInClient.",
                     EstablishTrustInClient.value, compoundSecMechConfig.getAs_mech().getRequires());
        /* Validate authentication mechanism is instance of ClientLTPAMechConfig */
        assertTrue("The authentication layer must be enabled with type ClientLTPAMechConfig.",
                   compoundSecMechConfig.getAs_mech() instanceof ClientLTPAMechConfig);
    }

    /**
     * Test authentication layer configured with EstablishTrustInClient: Required and mechanism: GSSUP and LTPA
     * This should create a 2 element list of CSSCompoundSecMechConfig object
     * Verifying each element required value is set to EstablishTrustInClient.value
     * Verifying ClientLTPAMechConfig and CSSGSSUPMechConfigDynamic object created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerGSSUPAndLTPA() throws Exception {
        putAuthLayerProperties(LTPA_GSSUP);
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Required");

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();

        assertEquals("There must be two compound sec mechs in the list.", 2, compoundSecMechList.size());

        /* Validate EstablishTrustInClient: required. */
        assertEquals("The authentication layer must require EstablishTrustInClient.",
                     EstablishTrustInClient.value, cssConfig.getMechList().mechAt(0).getAs_mech().getRequires());
        assertEquals("The authentication layer must require EstablishTrustInClient.",
                     EstablishTrustInClient.value, cssConfig.getMechList().mechAt(1).getAs_mech().getRequires());

        /* Validate authentication mechanism is instance of ClientTPAMechConfig */
        assertTrue("The authentication layer must be enabled with type ClientLTPAMechConfig.",
                   cssConfig.getMechList().mechAt(0).getAs_mech() instanceof ClientLTPAMechConfig);

        /* Validate authentication mechanism is instance of CSSGSSUPMechConfigDynamic */
        assertTrue("The authentication layer must be enabled with type CSSGSSUPMechConfigDynamic.",
                   cssConfig.getMechList().mechAt(1).getAs_mech() instanceof CSSGSSUPMechConfigDynamic);
    }

    /**
     * Test authentication layer configured with invalid mechanism (other than GSSUP and LTPA)
     * This should create a CSSNULLASMechConfig object
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerInvalidMechanism() throws Exception {
        putAuthLayerProperties(INVALID_AND_LTPA_MECH);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate authentication mechanism is instance of ClientLTPAMechConfig */
        assertTrue("The authentication layer must be enabled with type CSSNULLASMechConfig.",
                   cssConfig.getMechList().mechAt(0).getAs_mech() instanceof ClientLTPAMechConfig);
        assertTrue("Expected message was not logged", outputMgr.checkForMessages("CWWKS9600E:"));
    }

    @Test
    public void testAuthenticationLayerNullMechanism() throws Exception {
        putAuthLayerProperties(EMPTY_MECH);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate authentication mechanism is instance of CSSNULLASMechConfig */
        assertTrue("The authentication layer must be enabled with type CSSNULLASMechConfig.",
                   cssConfig.getMechList().mechAt(0).getAs_mech() instanceof CSSNULLASMechConfig);
        assertTrue("Expected message was not logged", outputMgr.checkForMessages("CWWKS9601W:"));
    }

    /**
     * Test authentication layer configured with EstablishTrustInClient: Never
     * This should create a CSSNULLASMechConfig object
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerETICNever() throws Exception {
        putAuthLayerProperties(LTPA_GSSUP);
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Never");

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate authentication mechanism is instance of CSSNULLASMechConfig */
        assertTrue("The authentication layer must be enabled with type CSSNULLASMechConfig.",
                   cssConfig.getMechList().mechAt(0).getAs_mech() instanceof CSSNULLASMechConfig);
    }

    private void putAuthLayerProperties(String[] mechanism) {
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyAuthenticationLayer");
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.mechanisms", mechanism);
        properties.put("clientPolicy.1.layers.0.authenticationLayer.0.establishTrustInClient", "Supported");
    }

    @Test
    public void testAttributeLayer() throws Exception {
        putAttributeLayerProperties();
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", true);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();
        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        CSSSASMechConfig cssSASMechConfig = compoundSecMechConfig.getSas_mech();

        assertEquals("The attribute layer must support identity assertion.",
                     IdentityAssertion.value, cssSASMechConfig.getSupports());
        assertEquals("The trusted identity must be set.", TRUSTED_ID, cssSASMechConfig.getTrustedIdentity());
        assertEquals("The trusted identity password must be set.",
                     TRUSTED_PWD, new String(cssSASMechConfig.getTrustedPassword().getChars()));
    }

    @Test
    public void testAttributeLayer_supportedIdentitiesTypes() throws Exception {
        putAttributeLayerProperties();
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", true);

        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);
        CSSCompoundSecMechListConfig compoundSecMechList = cssConfig.getMechList();
        CSSCompoundSecMechConfig compoundSecMechConfig = compoundSecMechList.mechAt(0);
        CSSSASMechConfig cssSASMechConfig = compoundSecMechConfig.getSas_mech();
        assertEquals("The trusted identity token must be set.",
                     ITTAnonymous.value | ITTPrincipalName.value | ITTDistinguishedName.value, cssSASMechConfig.getSupportedIdentityTypes());
    }

    /**
     * Test authentication layer configured with mechanism: GSSUP and gssup
     * This should create only one element list of CSSCompoundSecMechConfig object
     * Verifying the only one element is created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerLowerAndUpperCaseMechanism_GSSUP() throws Exception {
        putAuthLayerProperties(GSSUP_GSSUP);
        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate Mechanism: GSSUP. */
        assertEquals("The authentication layer must be enabled with type CSSGSSUPMechConfigDynamic.",
                     1, cssConfig.getMechList().size());
    }

    /**
     * Test authentication layer configured with mechanism: LTPA and ltpa
     * This should create only one element list of CSSCompoundSecMechConfig object
     * Verifying the only one element is created
     *
     * @throws Exception
     */
    @Test
    public void testAuthenticationLayerLowerAndUpperCaseMechanism_LTPA() throws Exception {
        putAuthLayerProperties(LTPA_LTPA);
        CSSConfig cssConfig = clientConfigHelper.getCSSConfig(properties);

        /* Validate Mechanism: LTPA. */
        assertEquals("The authentication layer must be enabled with type ClientLTPAMechConfig.",
                     1, cssConfig.getMechList().size());
    }

    private void putAttributeLayerProperties() {
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.config.referenceType", "com.ibm.ws.security.csiv2.clientPolicyAttributeLayer");
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.identityAssertionEnabled", false);
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.identityAssertionTypes", new String[] { "ITTAnonymous", "ITTPrincipalName", "ITTDistinguishedName" });
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.trustedIdentity", TRUSTED_ID);
        properties.put("clientPolicy.1.layers.0.attributeLayer.0.trustedPassword", new SerializableProtectedString(TRUSTED_PWD.toCharArray()));
    }

}

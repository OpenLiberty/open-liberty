/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.binding;

import javax.xml.namespace.QName;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.opensaml.common.SAMLObject;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.ws.message.handler.HandlerChainResolver;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.OutTransport;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.security.credential.Credential;

import test.common.SharedOutputManager;

/**
 * Unit test for @link {@link DebugMessageContext} class.
 */
public class DebugMessageContextTest {

    /**
     * Fake class to test {@link DebugMessageContext}.
     */
    private static class DebugMessageContextImpl extends DebugMessageContext<SAMLObject, SAMLObject, SAMLObject> {

        static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
        @Rule
        public TestRule managerRule = outputMgr;

        /*
         * (non-Javadoc)
         *
         * @see org.opensaml.common.binding.SAMLMessageContext#getInboundSAMLProtocol()
         */
        @Override
        public String getInboundSAMLProtocol() {
            // TODO Auto-generated method stub
            return null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.opensaml.common.binding.SAMLMessageContext#setInboundSAMLProtocol(java.lang.String)
         */
        @Override
        public void setInboundSAMLProtocol(String arg0) {
            // TODO Auto-generated method stub

        }

        /*
         * (non-Javadoc)
         *
         * @see org.opensaml.common.binding.SAMLMessageContext#setPeerEntityRole(javax.xml.namespace.QName)
         */
        @Override
        public void setPeerEntityRole(QName arg0) {
            // TODO Auto-generated method stub

        }

    }

    public static final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final DebugMessageContextImpl debugMessageContext = new DebugMessageContextImpl();
    private static final SAMLObject SAML_OBJECT_MCK = mockery.mock(SAMLObject.class);
    private static final EntityDescriptor ENTITY_DESCRIPTOR_MCK = mockery.mock(EntityDescriptor.class);
    private static final RoleDescriptor ROLE_DESCRIPTOR_MCK = mockery.mock(RoleDescriptor.class);
    private static final MetadataProvider METADATA_PROVIDER_MCK = mockery.mock(MetadataProvider.class);
    private static final Credential CREDENTIAL_MCK = mockery.mock(Credential.class);
    private static final Endpoint ENDPOINT_MCK = mockery.mock(Endpoint.class);
    private static final InTransport IN_TRANSPORT_MCK = mockery.mock(InTransport.class);
    private static final XMLObject XML_OBJECT_MCK = mockery.mock(XMLObject.class);
    private static final OutTransport OUT_TRANSPORT_MCK = mockery.mock(OutTransport.class);
    private static final SecurityPolicyResolver SECURITY_POLICY_RESOLVER_MCK = mockery.mock(SecurityPolicyResolver.class);
    private static final HandlerChainResolver HANDLER_CHAIN_RESOLVER_MCK = mockery.mock(HandlerChainResolver.class);

    @BeforeClass
    public static void setUp() {
        DebugMessageContextImpl.outputMgr.trace("*=all");
    }

    @AfterClass
    public static void tearDown() {
        DebugMessageContextImpl.outputMgr.trace("*=all=disabled");
    }

    /**
     * Test that {@link DebugMessageContext#isIssuerAuthenticated()} return the same value as {@link DebugMessageContext#isInboundSAMLMessageAuthenticated()}
     */
    @Test
    public void isIssuerAuthenticatedShouldReturnSameValueAsIsInboundSAMLMessageAuthenticatedMethod() {

        Assert.assertEquals(debugMessageContext.isInboundSAMLMessageAuthenticated(), debugMessageContext.isIssuerAuthenticated());
    }

    /**
     * This method verify if {@link DebugMessageContext#setCommunicationProfileId(String)} method applies the
     * {@link org.opensaml.xml.util.DatatypeHelper#safeTrimOrNullString(String)} method when setting the id parameter. Fails if the trim operation is not applied.
     */
    @Test
    public void setCommunicationProfileIdShouldApplySafeTrimOrNullStringToId() {

        debugMessageContext.setCommunicationProfileId("  ");
        Assert.assertNull(debugMessageContext.getCommunicationProfileId());

        debugMessageContext.setCommunicationProfileId(null);
        Assert.assertNull(debugMessageContext.getCommunicationProfileId());

        debugMessageContext.setCommunicationProfileId(" test value ");
        Assert.assertEquals("test value", debugMessageContext.getCommunicationProfileId());
    }

    /**
     * This method verify if {@link DebugMessageContext#setLocalEntityId(String)} method applies the {@link org.opensaml.xml.util.DatatypeHelper#safeTrimOrNullString(String)}
     * method when setting the id parameter. Fails if the trim operation is not applied.
     */
    @Test
    public void setLocalEntityIdShouldApplySafeTrimOrNullStringToId() {
        debugMessageContext.setLocalEntityId("  ");
        Assert.assertEquals(null, debugMessageContext.getLocalEntityId());

        debugMessageContext.setLocalEntityId(null);
        Assert.assertEquals(null, debugMessageContext.getLocalEntityId());

        debugMessageContext.setLocalEntityId(" test value ");
        Assert.assertEquals("test value", debugMessageContext.getLocalEntityId());
    }

    /**
     * This method verify if {@link DebugMessageContext#setOutboundMessageArtifactType(byte[])} method set a clone of the provided parameter if it's not null.
     * Fails if the getter return a null object.
     */
    @Test
    public void setOutboundMessageArtifactTypeShouldCloneIfTypeIsNotNull() {
        debugMessageContext.setOutboundMessageArtifactType("test value".getBytes());
        Assert.assertArrayEquals("test value".getBytes(), debugMessageContext.getOutboundMessageArtifactType());
    }

    /**
     * This method verify if {@link DebugMessageContext#setOutboundMessageArtifactType(byte[])} method set a null reference if the provided parameter is null.
     * Fails if the getter return a non null object.
     */
    @Test
    public void setOutboundMessageArtifactTypeShouldReturnNullIfTypeIsNull() {
        debugMessageContext.setOutboundMessageArtifactType(null);
        Assert.assertNull(debugMessageContext.getOutboundMessageArtifactType());
    }

    /**
     * This method verify if {@link DebugMessageContext#setOutboundSAMLMessageId(String)} method applies the
     * {@link org.opensaml.xml.util.DatatypeHelper#safeTrimOrNullString(String)} method when setting the id parameter. Fails if the trim operation is not applied.
     */
    @Test
    public void setOutboundSAMLMessageIdShouldApplySafeTrimOrNullStringToId() {
        debugMessageContext.setOutboundSAMLMessageId("  ");
        Assert.assertNull(debugMessageContext.getOutboundSAMLMessageId());

        debugMessageContext.setOutboundSAMLMessageId(null);
        Assert.assertNull(debugMessageContext.getOutboundSAMLMessageId());

        debugMessageContext.setOutboundSAMLMessageId(" test value ");
        Assert.assertEquals("test value", debugMessageContext.getOutboundSAMLMessageId());
    }

    /**
     * This method verify if {@link DebugMessageContext#setOutboundSAMLProtocol(String)} method applies the
     * {@link org.opensaml.xml.util.DatatypeHelper#safeTrimOrNullString(String)} method when setting the protocol parameter. Fails if the trim operation is not applied.
     */
    @Test
    public void setOutboundSAMLProtocolShouldApplySafeTrimOrNullStringToProtocolParameter() {
        debugMessageContext.setOutboundSAMLProtocol("  ");
        Assert.assertNull(debugMessageContext.getOutboundSAMLProtocol());

        debugMessageContext.setOutboundSAMLProtocol(null);
        Assert.assertNull(debugMessageContext.getOutboundSAMLProtocol());

        debugMessageContext.setOutboundSAMLProtocol(" test value ");
        Assert.assertEquals("test value", debugMessageContext.getOutboundSAMLProtocol());
    }

    /**
     * Test getter/setter method for the <code>inboundMessage<code> field.
     */
    @Test
    public void testGetterAndSetterOfInboundMessage() {
        debugMessageContext.setInboundMessage(SAML_OBJECT_MCK);
        Assert.assertEquals(SAML_OBJECT_MCK, debugMessageContext.getInboundMessage());
    }

    /**
     * Test getter/setter method for the <code>inboundMessageIssuer<code> field.
     */
    @Test
    public void testGetterAndSetterOfInboundMessageIssuer() {
        debugMessageContext.setInboundMessageIssuer("test value");;
        Assert.assertEquals("test value", debugMessageContext.getInboundMessageIssuer());
    }

    /**
     * Test getter/setter method for the <code>inboundSAMLMessage<code>field.
     */
    @Test
    public void testGetterAndSetterOfInboundSAMLMessage() {
        debugMessageContext.setInboundSAMLMessage(SAML_OBJECT_MCK);
        Assert.assertEquals(SAML_OBJECT_MCK, debugMessageContext.getInboundSAMLMessage());
    }

    /**
     * Test getter/setter method for the <code>inboundSAMLMessageAuthenticated<code> field.
     */
    @Test
    public void testGetterAndSetterOfInboundSAMLMessageAuthenticated() {
        debugMessageContext.setInboundSAMLMessageAuthenticated(true);
        Assert.assertEquals(true, debugMessageContext.isInboundSAMLMessageAuthenticated());
    }

    /**
     * Test getter/setter method for the <code>inboundSAMLMessageId<code>field.
     */
    @Test
    public void testGetterAndSetterOfInboundSAMLMessageId() {
        debugMessageContext.setInboundSAMLMessageId("id");
        Assert.assertEquals("id", debugMessageContext.getInboundSAMLMessageId());
    }

    /**
     * Test getter/setter method for the <code>inboundSAMLMessageIssueInstant<code>field.
     */
    @Test
    public void testGetterAndSetterOfInboundSAMLMessageIssueInstant() {
        DateTimeFormatter dateFormater = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTime date = dateFormater.parseDateTime("2004-12-05T09:22:05");

        debugMessageContext.setInboundSAMLMessageIssueInstant(date);
        Assert.assertEquals(date, debugMessageContext.getInboundSAMLMessageIssueInstant());
    }

    /**
     * Test getter/setter method for the <code>inboundTransport<code> field.
     */
    @Test
    public void testGetterAndSetterOfInboundTransport() {
        debugMessageContext.setInboundMessageTransport(IN_TRANSPORT_MCK);
        Assert.assertEquals(IN_TRANSPORT_MCK, debugMessageContext.getInboundMessageTransport());
    }

    /**
     * Test getter/setter method for the <code>localEntityMetadata<code>field.
     */
    @Test
    public void testGetterAndSetterOfLocalEntityMetadata() {

        debugMessageContext.setLocalEntityMetadata(ENTITY_DESCRIPTOR_MCK);
        Assert.assertEquals(ENTITY_DESCRIPTOR_MCK, debugMessageContext.getLocalEntityMetadata());
    }

    /**
     * Test getter/setter method for the <code>localEntityRole<code>field.
     */
    @Test
    public void testGetterAndSetterOfLocalEntityRole() {
        QName qname = new QName("testValue");

        debugMessageContext.setLocalEntityRole(qname);
        Assert.assertEquals(qname, debugMessageContext.getLocalEntityRole());
    }

    /**
     * Test getter/setter method for the <code>localEntityRoleMetadata<code>field.
     */
    @Test
    public void testGetterAndSetterOfLocalEntityRoleMetadata() {
        debugMessageContext.setLocalEntityRoleMetadata(ROLE_DESCRIPTOR_MCK);
        Assert.assertEquals(ROLE_DESCRIPTOR_MCK, debugMessageContext.getLocalEntityRoleMetadata());
    }

    /**
     * Test getter/setter method for the <code>metadataProvider<code>field.
     */
    @Test
    public void testGetterAndSetterOfMetadataProvider() {
        debugMessageContext.setMetadataProvider(METADATA_PROVIDER_MCK);
        Assert.assertEquals(METADATA_PROVIDER_MCK, debugMessageContext.getMetadataProvider());
    }

    /**
     * Test getter/setter method for the <code>ouboundSAMLMessageSigningCredential<code>field.
     */
    @Test
    public void testGetterAndSetterOfOuboundSAMLMessageSigningCredential() {
        debugMessageContext.setOutboundSAMLMessageSigningCredential(CREDENTIAL_MCK);
        Assert.assertEquals(CREDENTIAL_MCK, debugMessageContext.getOuboundSAMLMessageSigningCredential());
    }

    /**
     * Test getter/setter method for the <code>outboundHandlerChainResolver<code> field.
     */
    @Test
    public void testGetterAndSetterOfOutboundHandlerChainResolver() {
        debugMessageContext.setOutboundHandlerChainResolver(HANDLER_CHAIN_RESOLVER_MCK);
        Assert.assertEquals(HANDLER_CHAIN_RESOLVER_MCK, debugMessageContext.getOutboundHandlerChainResolver());
    }

    /**
     * Test getter/setter method for the <code>outboundMessage<code> field.
     */
    @Test
    public void testGetterAndSetterOfOutboundMessage() {
        debugMessageContext.setOutboundMessage(XML_OBJECT_MCK);
        Assert.assertEquals(XML_OBJECT_MCK, debugMessageContext.getOutboundMessage());
    }

    /**
     * Test getter/setter method for the <code>outboundMessageIssuer<code> field.
     */
    @Test
    public void testGetterAndSetterOfOutboundMessageIssuer() {
        debugMessageContext.setOutboundMessageIssuer("test value");
        Assert.assertEquals("test value", debugMessageContext.getOutboundMessageIssuer());
    }

    /**
     * Test getter/setter method for the <code>outboundSAMLMessage<code>field.
     */
    @Test
    public void testGetterAndSetterOfOutboundSAMLMessage() {
        debugMessageContext.setOutboundSAMLMessage(SAML_OBJECT_MCK);
        Assert.assertEquals(SAML_OBJECT_MCK, debugMessageContext.getOutboundSAMLMessage());
    }

    /**
     * Test getter/setter method for the <code>outboundSAMLMessageIssueInstant<code>field.
     */
    @Test
    public void testGetterAndSetterOfOutboundSAMLMessageIssueInstant() {
        DateTimeFormatter dateFormater = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss");
        DateTime date = dateFormater.parseDateTime("2004-12-05T09:22:05");

        debugMessageContext.setOutboundSAMLMessageIssueInstant(date);
        Assert.assertEquals(date, debugMessageContext.getOutboundSAMLMessageIssueInstant());
    }

    /**
     * Test getter/setter method for the <code>outboundTransport<code> field.
     */
    @Test
    public void testGetterAndSetterOfOutboundTransport() {
        debugMessageContext.setOutboundMessageTransport(OUT_TRANSPORT_MCK);;
        Assert.assertEquals(OUT_TRANSPORT_MCK, debugMessageContext.getOutboundMessageTransport());
    }

    /**
     * Test getter/setter method for the <code>peerEntityEndpoint<code>field.
     */
    @Test
    public void testGetterAndSetterOfPeerEntityEndpoint() {
        debugMessageContext.setPeerEntityEndpoint(ENDPOINT_MCK);
        Assert.assertEquals(ENDPOINT_MCK, debugMessageContext.getPeerEntityEndpoint());
    }

    /**
     * Test getter/setter method for the <code>peerEntityId<code>field.
     */
    @Test
    public void testGetterAndSetterOfPeerEntityId() {
        debugMessageContext.setPeerEntityId("testId");
        Assert.assertEquals("testId", debugMessageContext.getPeerEntityId());
    }

    /**
     * Test getter/setter method for the <code>peerEntityMetadata<code>field.
     */
    @Test
    public void testGetterAndSetterOfPeerEntityMetadata() {
        debugMessageContext.setPeerEntityMetadata(ENTITY_DESCRIPTOR_MCK);
        Assert.assertEquals(ENTITY_DESCRIPTOR_MCK, debugMessageContext.getPeerEntityMetadata());
    }

    /**
     * Test getter/setter method for the <code>peerEntityRoleMetadata<code>field.
     */
    @Test
    public void testGetterAndSetterOfPeerEntityRoleMetadata() {
        debugMessageContext.setPeerEntityRoleMetadata(ROLE_DESCRIPTOR_MCK);
        Assert.assertEquals(ROLE_DESCRIPTOR_MCK, debugMessageContext.getPeerEntityRoleMetadata());
    }

    /**
     * Test getter/setter method for the <code>postSecurityInboundHandlerChainResolver<code> field.
     */
    @Test
    public void testGetterAndSetterOfPostSecurityInboundHandlerChainResolver() {
        debugMessageContext.setPostSecurityInboundHandlerChainResolver(HANDLER_CHAIN_RESOLVER_MCK);
        Assert.assertEquals(HANDLER_CHAIN_RESOLVER_MCK, debugMessageContext.getPostSecurityInboundHandlerChainResolver());
    }

    /**
     * Test getter/setter method for the <code>preSecurityInboundHandlerChainResolver<code> field.
     */
    @Test
    public void testGetterAndSetterOfPreSecurityInboundHandlerChainResolver() {
        debugMessageContext.setPreSecurityInboundHandlerChainResolver(HANDLER_CHAIN_RESOLVER_MCK);
        Assert.assertEquals(HANDLER_CHAIN_RESOLVER_MCK, debugMessageContext.getPreSecurityInboundHandlerChainResolver());
    }

    /**
     * Test getter/setter method for the <code>relayState<code> field.
     */
    @Test
    public void testGetterAndSetterOfRelayState() {
        debugMessageContext.setRelayState("test value");
        Assert.assertEquals("test value", debugMessageContext.getRelayState());
    }

    /**
     * Test getter/setter method for the <code>securityPolicyResolver<code> field.
     */
    @Test
    public void testGetterAndSetterOfSecurityPolicyResolver() {
        debugMessageContext.setSecurityPolicyResolver(SECURITY_POLICY_RESOLVER_MCK);
        Assert.assertEquals(SECURITY_POLICY_RESOLVER_MCK, debugMessageContext.getSecurityPolicyResolver());
    }

    /**
     * Test getter/setter method for the <code>subjectNameIdentifier<code> field.
     */
    @Test
    public void testGetterAndSetterOfSubjectNameIdentifier() {
        debugMessageContext.setSubjectNameIdentifier(SAML_OBJECT_MCK);
        Assert.assertEquals(SAML_OBJECT_MCK, debugMessageContext.getSubjectNameIdentifier());
    }

    /**
     * Test getter method for the <code>peerentityRole<code>field.
     */
    @Test
    public void testGetterOfPeerEntityRole() {
        Assert.assertNull(debugMessageContext.getPeerEntityRole()); //Null because DebugMessageContext abstract class does not provide abstract implementation
    }

}

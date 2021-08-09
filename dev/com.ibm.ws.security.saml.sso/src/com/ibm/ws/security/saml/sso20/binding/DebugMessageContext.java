/*
 * Licensed to the University Corporation for Advanced Internet Development,
 * Inc. (UCAID) under one or more contributor license agreements.  See the
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.ws.security.saml.sso20.binding;

import javax.xml.namespace.QName;

import org.joda.time.DateTime;
import org.opensaml.common.SAMLObject;
import org.opensaml.common.binding.SAMLMessageContext;
import org.opensaml.saml2.metadata.Endpoint;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.saml2.metadata.RoleDescriptor;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.ws.message.handler.HandlerChainResolver;
import org.opensaml.ws.security.SecurityPolicyResolver;
import org.opensaml.ws.transport.InTransport;
import org.opensaml.ws.transport.OutTransport;
import org.opensaml.xml.XMLObject; // MessageContext
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.util.DatatypeHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.saml.TraceConstants;
import com.ibm.ws.security.saml.sso20.internal.utils.SamlUtil;

/**
 * Base implemention of {@link SAMLMessageContext}.
 *
 * @param <InboundMessageType>  type of inbound SAML message
 * @param <OutboundMessageType> type of outbound SAML message
 * @param <NameIdentifierType>  type of name identifier used for subjects
 */
public abstract class DebugMessageContext<InboundMessageType extends SAMLObject, OutboundMessageType extends SAMLObject, NameIdentifierType extends SAMLObject> implements SAMLMessageContext<InboundMessageType, OutboundMessageType, NameIdentifierType> {
    // This DebugMessageContext is not in use in the product code.
    // But it helps during the debugging. It print out extra message during runtime
    //   which is helpful in the tough debugging.
    // The MessageContext is implemented in the second part below
    public static final TraceComponent tc = Tr.register(DebugMessageContext.class,
                                                        TraceConstants.TRACE_GROUP,
                                                        TraceConstants.MESSAGE_BUNDLE);

    /** Gets the artifact type used for outbound messages. */
    private byte[] artifactType;

    /** Name identifier for the Subject of the message. */
    private NameIdentifierType subjectNameIdentifier;

    /** Local entity's ID. */
    private String localEntityId;

    /** Local entity's metadata. */
    private EntityDescriptor localEntityMetadata;

    /** Asserting entity's role. */
    private QName localEntityRole;

    /** Asserting entity's role metadata. */
    private RoleDescriptor localEntityRoleMetadata;

    /** Inbound SAML message. */
    private InboundMessageType inboundSAMLMessage;

    /** Whether the inbound SAML message has been authenticated. */
    private boolean inboundSAMLMessageAuthenticated;

    /** Inbound SAML message's ID. */
    private String inboundSAMLMessageId;

    /** Inbound SAML message's issue instant. */
    private DateTime inboundSAMLMessageIssueInstant;

    /** Inbound SAML protocol. */
    @SuppressWarnings("unused")
    private String inboundSAMLProtocol;

    /** Metadata provider used to lookup entity information. */
    private MetadataProvider metadataProvider;

    /** Outbound SAML message. */
    private OutboundMessageType outboundSAMLMessage;

    /** Outbound SAML message's ID. */
    private String outboundSAMLMessageId;

    /** Outbound SAML message's issue instant. */
    private DateTime outboundSAMLMessageIssueInstant;

    /** Outboud SAML message signing credential. */
    private Credential outboundSAMLMessageSigningCredential;

    /** Outbound SAML procotol. */
    private String outboundSAMLProtocol;

    /** Message relay state. */
    private String relayState;

    /** Peer entity's endpoint. */
    private Endpoint peerEntityEndpoint;

    /** Peer entity's ID. */
    private String peerEntityId;

    /** Peer entity's metadata. */
    private EntityDescriptor peerEntityMetadata;

    /** Peer entity's role. */
    private QName peerEntityRole;

    /** Peer entity's role metadata. */
    private RoleDescriptor peerEntityRoleMetadata;

    /** {@inheritDoc} */
    @Override
    public InboundMessageType getInboundSAMLMessage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundSAMLMessage:"
                         + inboundSAMLMessage);
        }
        checkNullObject(inboundSAMLMessage);
        return inboundSAMLMessage;
    }

    /** {@inheritDoc} */
    @Override
    public String getInboundSAMLMessageId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundSAMLMessageId:"
                         + inboundSAMLMessageId);
        }
        checkNullObject(inboundSAMLMessageId);
        return inboundSAMLMessageId;
    }

    /** {@inheritDoc} */
    @Override
    public DateTime getInboundSAMLMessageIssueInstant() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundSAMLMessageIssueInstant:"
                         + inboundSAMLMessageIssueInstant);
        }
        checkNullObject(inboundSAMLMessageIssueInstant);
        return inboundSAMLMessageIssueInstant;
    }

    /** {@inheritDoc} */
    @Override
    public String getLocalEntityId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getLocalEntityId:"
                         + localEntityId);
        }
        checkNullObject(localEntityId);
        return localEntityId;
    }

    /** {@inheritDoc} */
    @Override
    public EntityDescriptor getLocalEntityMetadata() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getLocalEntityMetadata:"
                         + localEntityMetadata);
        }
        checkNullObject(localEntityMetadata);
        return localEntityMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public QName getLocalEntityRole() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getLocalEntityRole:"
                         + localEntityRole);
        }
        checkNullObject(localEntityRole);
        return localEntityRole;
    }

    /** {@inheritDoc} */
    @Override
    public RoleDescriptor getLocalEntityRoleMetadata() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getLocalEntityRoleMetada:"
                         + localEntityRoleMetadata);
        }
        checkNullObject(localEntityRoleMetadata);
        return localEntityRoleMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public MetadataProvider getMetadataProvider() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getMetadaProvider:"
                         + metadataProvider);
        }
        checkNullObject(metadataProvider);
        return metadataProvider;
    }

    /** typo */
    /** {@inheritDoc} */
    @Override
    public Credential getOuboundSAMLMessageSigningCredential() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOuboundSAMLMessageSigningCredential:"
                         + outboundSAMLMessageSigningCredential);
        }
        checkNullObject(outboundSAMLMessageSigningCredential);
        return outboundSAMLMessageSigningCredential;
    }

    /** {@inheritDoc} */
    @Override
    public OutboundMessageType getOutboundSAMLMessage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundSAMLMessage:"
                         + outboundSAMLMessage);
        }
        checkNullObject(outboundSAMLMessage);
        return outboundSAMLMessage;
    }

    /** {@inheritDoc} */
    @Override
    public String getOutboundSAMLMessageId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundSAMLMessageId:"
                         + outboundSAMLMessageId);
        }
        checkNullObject(outboundSAMLMessageId);
        return outboundSAMLMessageId;
    }

    /** {@inheritDoc} */
    @Override
    public DateTime getOutboundSAMLMessageIssueInstant() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundSAMLMessageIssueInstant:"
                         + outboundSAMLMessageIssueInstant);
        }
        checkNullObject(outboundSAMLMessageIssueInstant);
        return outboundSAMLMessageIssueInstant;
    }

    /** {@inheritDoc} */
    @Override
    public String getOutboundSAMLProtocol() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundSAMLProtocol:"
                         + outboundSAMLProtocol);
        }
        checkNullObject(outboundSAMLProtocol);
        return outboundSAMLProtocol;
    }

    /** {@inheritDoc} */
    @Override
    public Endpoint getPeerEntityEndpoint() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPeerEntityEndpoint:"
                         + peerEntityEndpoint);
        }
        checkNullObject(peerEntityEndpoint);
        return peerEntityEndpoint;
    }

    /** {@inheritDoc} */
    @Override
    public String getPeerEntityId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPeerEntityId:"
                         + peerEntityId);
        }
        checkNullObject(peerEntityId);
        return peerEntityId;
    }

    /** {@inheritDoc} */
    @Override
    public EntityDescriptor getPeerEntityMetadata() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPeerEntityMetadata:"
                         + peerEntityMetadata);
        }
        //checkNullObject(peerEntityMetadata);
        return peerEntityMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public QName getPeerEntityRole() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPeerEntityRole:"
                         + peerEntityRole);
        }
        //checkNullObject(peerEntityRole);
        return peerEntityRole;
    }

    /** {@inheritDoc} */
    @Override
    public RoleDescriptor getPeerEntityRoleMetadata() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPeerEntityRoleMetadata:"
                         + peerEntityRoleMetadata);
        }
        checkNullObject(peerEntityRoleMetadata);
        return peerEntityRoleMetadata;
    }

    /** {@inheritDoc} */
    @Override
    public String getRelayState() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getRelayState:"
                         + relayState);
        }
        checkNullObject(relayState);
        return relayState;
    }

    /** {@inheritDoc} */
    @Override
    public NameIdentifierType getSubjectNameIdentifier() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getSubjectNameIdentifier:"
                         + subjectNameIdentifier);
        }
        checkNullObject(subjectNameIdentifier);
        return subjectNameIdentifier;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInboundSAMLMessageAuthenticated() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:isInboundSAMLMessageAuthenticatedr:"
                         + inboundSAMLMessageAuthenticated);
        }
        checkNullObject(inboundSAMLMessageAuthenticated);
        return inboundSAMLMessageAuthenticated;
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundSAMLMessage(InboundMessageType message) {
        inboundSAMLMessage = message;
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundSAMLMessageAuthenticated(boolean isAuthenticated) {
        inboundSAMLMessageAuthenticated = isAuthenticated;
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundSAMLMessageId(String id) {
        inboundSAMLMessageId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundSAMLMessageIssueInstant(DateTime instant) {
        inboundSAMLMessageIssueInstant = instant;
    }

    /** {@inheritDoc} */
    @Override
    public void setLocalEntityId(String id) {
        localEntityId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setLocalEntityMetadata(EntityDescriptor metadata) {
        localEntityMetadata = metadata;
    }

    /** {@inheritDoc} */
    @Override
    public void setLocalEntityRole(QName role) {
        localEntityRole = role;
    }

    /** {@inheritDoc} */
    @Override
    public void setLocalEntityRoleMetadata(RoleDescriptor role) {
        localEntityRoleMetadata = role;
    }

    /** {@inheritDoc} */
    @Override
    public void setMetadataProvider(MetadataProvider provider) {
        metadataProvider = provider;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundSAMLMessage(OutboundMessageType message) {
        outboundSAMLMessage = message;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundSAMLMessageId(String id) {
        outboundSAMLMessageId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundSAMLMessageIssueInstant(DateTime instant) {
        outboundSAMLMessageIssueInstant = instant;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundSAMLMessageSigningCredential(Credential credential) {
        outboundSAMLMessageSigningCredential = credential;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundSAMLProtocol(String protocol) {
        outboundSAMLProtocol = DatatypeHelper.safeTrimOrNullString(protocol);
    }

    /** {@inheritDoc} */
    @Override
    public void setPeerEntityEndpoint(Endpoint endpoint) {
        peerEntityEndpoint = endpoint;
    }

    /** {@inheritDoc} */
    @Override
    public void setPeerEntityId(String id) {
        peerEntityId = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setPeerEntityMetadata(EntityDescriptor metadata) {
        peerEntityMetadata = metadata;
    }

    /** {@inheritDoc} */
    @Override
    public void setPeerEntityRoleMetadata(RoleDescriptor role) {
        peerEntityRoleMetadata = role;
    }

    /** {@inheritDoc} */
    @Override
    public void setRelayState(String state) {
        relayState = DatatypeHelper.safeTrimOrNullString(state);
    }

    /** {@inheritDoc} */
    @Override
    public void setSubjectNameIdentifier(NameIdentifierType identifier) {
        subjectNameIdentifier = identifier;
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getOutboundMessageArtifactType() {
        if (artifactType != null) {
            checkNullObject(artifactType.clone());
            return artifactType.clone();
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundMessageArtifactType(byte[] type) {
        artifactType = type == null ? null : type.clone();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isIssuerAuthenticated() {
        checkNullObject(isInboundSAMLMessageAuthenticated());
        return isInboundSAMLMessageAuthenticated();
    }

    // The second part: Base MessageContext

    /** Unique id of the communication profile in use. */
    private String communicationProfile;

    /** The inbound message. */
    private XMLObject inboundMessage;

    /** Issuer of the inbound message. */
    private String inboundMessageIssuer;

    /** Inbound message transport. */
    private InTransport inboundTransport;

    /** Outbound message. */
    private XMLObject outboundMessage;

    /** Issuer of the outbound message. */
    private String outboundMessageIssuer;

    /** Outbound message transport. */
    private OutTransport outboundTransport;

    /** Resolver used to determine active security policy. */
    private SecurityPolicyResolver securityPolicyResolver;

    /** Pre-SecurityPolicy inbound handler chain. */
    private HandlerChainResolver preSecurityInboundHandlerChainResolver;

    /** Post-SecurityPolicy inbound handler chain. */
    private HandlerChainResolver postSecurityInboundHandlerChainResolver;

    /** Inbound handler chain. */
    private HandlerChainResolver outboundHandlerChainResolver;

    /** {@inheritDoc} */
    @Override
    public String getCommunicationProfileId() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getCommunicationProfileId(mc):"
                         + communicationProfile);
        }
        checkNullObject(communicationProfile);
        return communicationProfile;
    }

    /** {@inheritDoc} */
    @Override
    public XMLObject getInboundMessage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundMessage(mc):"
                         + inboundMessage);
        }
        checkNullObject(inboundMessage);
        return inboundMessage;
    }

    /** {@inheritDoc} */
    @Override
    public String getInboundMessageIssuer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundMessageIssuer(mc):"
                         + inboundMessageIssuer);
        }
        checkNullObject(inboundMessageIssuer);
        return inboundMessageIssuer;
    }

    /** {@inheritDoc} */
    @Override
    public InTransport getInboundMessageTransport() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getInboundMessageTransport(mc):"
                         + inboundTransport);
        }
        checkNullObject(inboundTransport);
        return inboundTransport;
    }

    /** {@inheritDoc} */
    @Override
    public XMLObject getOutboundMessage() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundMessage(mc):"
                         + outboundMessage);
        }
        checkNullObject(outboundMessage);
        return outboundMessage;
    }

    /** {@inheritDoc} */
    @Override
    public String getOutboundMessageIssuer() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundMessageIssuer(mc):"
                         + outboundMessageIssuer);
        }
        checkNullObject(outboundMessageIssuer);
        return outboundMessageIssuer;
    }

    /** {@inheritDoc} */
    @Override
    public OutTransport getOutboundMessageTransport() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundMessageTraneport(mc):"
                         + outboundTransport);
        }
        checkNullObject(outboundTransport);
        return outboundTransport;
    }

    /** {@inheritDoc} */
    @Override
    public SecurityPolicyResolver getSecurityPolicyResolver() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getSecurityPolicyResolver(mc):"
                         + securityPolicyResolver);
        }
        // checkNullObject(securityPolicyResolver);
        return securityPolicyResolver;
    }

    /** {@inheritDoc} */
    @Override
    public void setCommunicationProfileId(String id) {
        communicationProfile = DatatypeHelper.safeTrimOrNullString(id);
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundMessage(XMLObject message) {
        inboundMessage = message;
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundMessageIssuer(String issuer) {
        inboundMessageIssuer = issuer;
    }

    /** {@inheritDoc} */
    @Override
    public void setInboundMessageTransport(InTransport transport) {
        inboundTransport = transport;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundMessage(XMLObject message) {
        outboundMessage = message;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundMessageIssuer(String issuer) {
        outboundMessageIssuer = issuer;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundMessageTransport(OutTransport transport) {
        outboundTransport = transport;
    }

    /** {@inheritDoc} */
    @Override
    public void setSecurityPolicyResolver(SecurityPolicyResolver resolver) {
        securityPolicyResolver = resolver;
    }

    // duplicated in MessageContext and SAMLMessageContext
    ///** {@inheritDoc} */
    //public boolean isIssuerAuthenticated() {
    //        return getInboundMessageTransport().isAuthenticated();
    //}

    /** {@inheritDoc} */
    @Override
    public HandlerChainResolver getPreSecurityInboundHandlerChainResolver() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPreSecurityInboundHandlerChainResolver(mc):"
                         + preSecurityInboundHandlerChainResolver);
        }
        checkNullObject(preSecurityInboundHandlerChainResolver);
        return preSecurityInboundHandlerChainResolver;
    }

    /** {@inheritDoc} */
    @Override
    public HandlerChainResolver getPostSecurityInboundHandlerChainResolver() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getPostSecurityInboundHandlerChainResolver(mc):"
                         + postSecurityInboundHandlerChainResolver);
        }
        checkNullObject(postSecurityInboundHandlerChainResolver);
        return postSecurityInboundHandlerChainResolver;
    }

    /** {@inheritDoc} */
    @Override
    public HandlerChainResolver getOutboundHandlerChainResolver() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "BasicMessageContext:getOutboundHandlerChainResolver(mc):"
                         + outboundHandlerChainResolver);
        }
        checkNullObject(outboundHandlerChainResolver);
        return outboundHandlerChainResolver;
    }

    /** {@inheritDoc} */
    @Override
    public void setPreSecurityInboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
        preSecurityInboundHandlerChainResolver = newHandlerChainResolver;
    }

    /** {@inheritDoc} */
    @Override
    public void setPostSecurityInboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
        postSecurityInboundHandlerChainResolver = newHandlerChainResolver;
    }

    /** {@inheritDoc} */
    @Override
    public void setOutboundHandlerChainResolver(HandlerChainResolver newHandlerChainResolver) {
        outboundHandlerChainResolver = newHandlerChainResolver;
    }

    @Trivial
    private void checkNullObject(Object object) {
        // debug only. Will be removed
        if (object == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Exception e = new Exception("Debugging Only");
                StringBuffer sb = SamlUtil.dumpStackTrace(e, -1);
                Tr.debug(tc, sb.toString());
            }
        }
    }

}
/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jmock.Mockery;
import org.jmock.States;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
//import org.opensaml.message.MessageContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
//import org.opensaml.saml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml.metadata.resolver.impl.DOMMetadataResolver;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.Condition;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.EncryptedAssertion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;

import com.ibm.websphere.security.saml2.Saml20Token;
import com.ibm.ws.security.common.structures.Cache;
import com.ibm.ws.security.saml.SsoConfig;
import com.ibm.ws.security.saml.SsoRequest;
import com.ibm.ws.security.saml.SsoSamlService;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContext;
import com.ibm.ws.security.saml.sso20.binding.BasicMessageContextBuilder;
import com.ibm.ws.security.saml.sso20.internal.utils.ForwardRequestInfo;
import com.ibm.ws.security.saml.sso20.internal.utils.UserData;
import com.ibm.ws.webcontainer.security.WebAppSecurityConfig;
import com.ibm.ws.webcontainer.security.WebProviderAuthenticatorHelper;
import com.ibm.wsspi.webcontainer.servlet.IExtendedRequest;

/**
 * Class that contains common objects under the tests.
 */
@SuppressWarnings("rawtypes")
public class CommonMockObjects {

    public static final String PROVIDER_ID = "providerId";
    public static final String keyServicePID = "keyServicePid";
    public static final String keyId = "keyId";
    public static final String SETUP = "setUp";
    public static final String SAML20_AUTHENTICATION_FAIL = "SAML20_AUTHENTICATION_FAIL";

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final WebProviderAuthenticatorHelper authHelper = mockery.mock(WebProviderAuthenticatorHelper.class);
    private final HttpServletRequest servletRequest = mockery.mock(IExtendedRequest.class);
    private final HttpServletResponse servletResponse = mockery.mock(HttpServletResponse.class);
    private final HttpSession session = mockery.mock(HttpSession.class);
    private final SsoSamlService ssoService = mockery.mock(SsoSamlService.class);
    private final SsoConfig ssoConfig = mockery.mock(SsoConfig.class);
    private final Saml20Token sso20Token = mockery.mock(Saml20Token.class);
    private final Cache cache = mockery.mock(Cache.class);
    private final WebAppSecurityConfig webAppSecConfig = mockery.mock(WebAppSecurityConfig.class);
    private final BasicMessageContext basicMessageContext = mockery.mock(BasicMessageContext.class);
    private final Assertion assertion = mockery.mock(Assertion.class);
    private final Issuer issuer = mockery.mock(Issuer.class);
    private final EntityDescriptor entityDescriptor = mockery.mock(EntityDescriptor.class);
    private final KeyInfoCredentialResolver keyInfoCredResolver = mockery.mock(KeyInfoCredentialResolver.class);
    private final DOMMetadataResolver metadataProvider = mockery.mock(DOMMetadataResolver.class);
    private final ForwardRequestInfo requestInfo = mockery.mock(ForwardRequestInfo.class);
    private final Signature signature = mockery.mock(Signature.class);
    private final MessageContext messageContext = mockery.mock(MessageContext.class);
    private final Response samlResponse = mockery.mock(Response.class);
    private final SsoRequest ssoRequest = mockery.mock(SsoRequest.class);
    private final UserData userData = mockery.mock(UserData.class);
    private final Subject subject = mockery.mock(Subject.class);
    private final SubjectConfirmation subjectConfirmation = mockery.mock(SubjectConfirmation.class);
    private final SubjectConfirmationData subjectConfirmationData = mockery.mock(SubjectConfirmationData.class);
    private final Status status = mockery.mock(Status.class);
    private final StatusCode statusCode = mockery.mock(StatusCode.class);
    private final Condition condition = mockery.mock(Condition.class);
    private final Conditions conditions = mockery.mock(Conditions.class);
    private final AuthnStatement authnStatement = mockery.mock(AuthnStatement.class);
    private final NameID nameId = mockery.mock(NameID.class);
    private final AudienceRestriction audienceRestriction = mockery.mock(AudienceRestriction.class);
    private final EncryptedAssertion encryptedAssertion = mockery.mock(EncryptedAssertion.class);
    private final BasicMessageContextBuilder basicMessageContextBuilder = mockery.mock(BasicMessageContextBuilder.class);
    private final SAMLPeerEntityContext samlPeerEntityContext = mockery.mock(SAMLPeerEntityContext.class);

    private final States stateMachine = mockery.states("states");

    /**
     * @return the samlPeerEntityContext
     */
    public SAMLPeerEntityContext getSAMLPeerEntityContext() {
        return samlPeerEntityContext;
    }

    /**
     * @return the metadataProvider
     */
    public DOMMetadataResolver getMetadataProvider() {
        return metadataProvider;
    }

    /**
     * @return the requestInfo
     */
    public ForwardRequestInfo getRequestInfo() {
        return requestInfo;
    }

    /**
     * @return the signature
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * @return the mockery
     */
    public Mockery getMockery() {
        return mockery;
    }

    /**
     * @return the authHelper
     */
    public WebProviderAuthenticatorHelper getAuthHelper() {
        return authHelper;
    }

    /**
     * @return the request
     */
    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    /**
     * @return the response
     */
    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    /**
     * @return the request
     */
    public HttpSession getSession() {
        return session;
    }

    /**
     * @return the ssoService
     */
    public SsoSamlService getSsoService() {
        return ssoService;
    }

    /**
     * @return the ssoConfig
     */
    public SsoConfig getSsoConfig() {
        return ssoConfig;
    }

    /**
     * @return the sso20Token
     */
    public Saml20Token getSso20Token() {
        return sso20Token;
    }

    /**
     * @return the cache
     */
    public Cache getCache() {
        return cache;
    }

    /**
     * @return the webAppSecConfig
     */
    public WebAppSecurityConfig getWebAppSecConfig() {
        return webAppSecConfig;
    }

    /**
     * @return the assertion
     */
    public Assertion getAssertion() {
        return assertion;
    }

    /**
     * @return the issuer
     */
    public Issuer getIssuer() {
        return issuer;
    }

    /**
     * @return the entityDescriptor
     */
    public EntityDescriptor getEntityDescriptor() {
        return entityDescriptor;
    }

    /**
     * @return the keyInfoCredResolver
     */
    public KeyInfoCredentialResolver getKeyInfoCredResolver() {
        return keyInfoCredResolver;
    }

    /**
     * @return the stateMachine
     */
    public States getStateMachine() {
        return stateMachine;
    }

    /**
     * @return the messageContext
     */
    public MessageContext getMessageContext() {
        return messageContext;
    }

    /**
     * @return the samlResponse
     */
    public Response getSamlResponse() {
        return samlResponse;
    }

    /**
     * @return the ssoRequest
     */
    public SsoRequest getSsoRequest() {
        return ssoRequest;
    }

    /**
     * @return the userData
     */
    public UserData getUserData() {
        return userData;
    }

    /**
     * @return the basicMessageContext
     */
    public BasicMessageContext<?, ?> getBasicMessageContext() {
        return basicMessageContext;
    }

    /**
     * @return the subject
     */
    public Subject getSubject() {
        return subject;
    }

    /**
     * @return the subjectConfirmation
     */
    public SubjectConfirmation getSubjectConfirmation() {
        return subjectConfirmation;
    }

    /**
     * @return the subjectConfirmationData
     */
    public SubjectConfirmationData getSubjectConfirmationData() {
        return subjectConfirmationData;
    }

    /**
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * @return the statusCode
     */
    public StatusCode getStatusCode() {
        return statusCode;
    }

    /**
     * @return the condition
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * @return the conditions
     */
    public Conditions getConditions() {
        return conditions;
    }

    /**
     * @return the authnStatement
     */
    public AuthnStatement getAuthnStatement() {
        return authnStatement;
    }

    /**
     * @return the nameId
     */
    public NameID getNameId() {
        return nameId;
    }

    /**
     * @return the audienceRestriction
     */
    public AudienceRestriction getAudienceRestriction() {
        return audienceRestriction;
    }

    /**
     * @return the encryptedAssertion
     */
    public EncryptedAssertion getEncryptedAssertion() {
        return encryptedAssertion;
    }

    /**
     * @return the basicMessageContextBuilder
     */
    public BasicMessageContextBuilder<?, ?, ?> getBasicMessageContextBuilder() {
        return basicMessageContextBuilder;
    }
}

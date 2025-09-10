/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.security.saml.sso20.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mockito.Mockito;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
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
 * Class that contains common objects under the tests using Mockito framework.
 */
@SuppressWarnings("rawtypes")
public class CommonMockitoObjects {

    public static final String PROVIDER_ID = "providerId";
    public static final String keyServicePID = "keyServicePid";
    public static final String keyId = "keyId";
    public static final String SETUP = "setUp";
    public static final String SAML20_AUTHENTICATION_FAIL = "SAML20_AUTHENTICATION_FAIL";

    private WebProviderAuthenticatorHelper authHelper;
    private IExtendedRequest servletRequest;
    private HttpServletResponse servletResponse;
    private HttpSession session;
    private SsoSamlService ssoService;
    private SsoConfig ssoConfig;
    private Saml20Token sso20Token;
    private Cache cache;
    private WebAppSecurityConfig webAppSecConfig;
    private BasicMessageContext basicMessageContext;
    private Assertion assertion;
    private Issuer issuer;
    private EntityDescriptor entityDescriptor;
    private KeyInfoCredentialResolver keyInfoCredResolver;
    private ForwardRequestInfo requestInfo;
    private Signature signature;
    private Response samlResponse;
    private Status status;
    private StatusCode statusCode;
    private Subject subject;
    private SubjectConfirmation subjectConfirmation;
    private SubjectConfirmationData subjectConfirmationData;
    private Condition condition;
    private Conditions conditions;
    private AuthnStatement authnStatement;
    private NameID nameId;
    private AudienceRestriction audienceRestriction;
    private EncryptedAssertion encryptedAssertion;
    private BasicMessageContextBuilder basicMessageContextBuilder;
    private MessageContext messageContext;
    private SAMLPeerEntityContext samlPeerEntityContext;
    private SsoRequest ssoRequest;

    public CommonMockitoObjects() {
        authHelper = Mockito.mock(WebProviderAuthenticatorHelper.class);
        servletRequest = Mockito.mock(IExtendedRequest.class);
        servletResponse = Mockito.mock(HttpServletResponse.class);
        session = Mockito.mock(HttpSession.class);
        ssoService = Mockito.mock(SsoSamlService.class);
        ssoConfig = Mockito.mock(SsoConfig.class);
        sso20Token = Mockito.mock(Saml20Token.class);
        cache = Mockito.mock(Cache.class);
        webAppSecConfig = Mockito.mock(WebAppSecurityConfig.class);
        basicMessageContext = Mockito.mock(BasicMessageContext.class);
        assertion = Mockito.mock(Assertion.class);
        issuer = Mockito.mock(Issuer.class);
        entityDescriptor = Mockito.mock(EntityDescriptor.class);
        keyInfoCredResolver = Mockito.mock(KeyInfoCredentialResolver.class);
        requestInfo = Mockito.mock(ForwardRequestInfo.class);
        signature = Mockito.mock(Signature.class);
        samlResponse = Mockito.mock(Response.class);
        status = Mockito.mock(Status.class);
        statusCode = Mockito.mock(StatusCode.class);
        subject = Mockito.mock(Subject.class);
        subjectConfirmation = Mockito.mock(SubjectConfirmation.class);
        subjectConfirmationData = Mockito.mock(SubjectConfirmationData.class);
        condition = Mockito.mock(Condition.class);
        conditions = Mockito.mock(Conditions.class);
        authnStatement = Mockito.mock(AuthnStatement.class);
        nameId = Mockito.mock(NameID.class);
        audienceRestriction = Mockito.mock(AudienceRestriction.class);
        encryptedAssertion = Mockito.mock(EncryptedAssertion.class);
        basicMessageContextBuilder = Mockito.mock(BasicMessageContextBuilder.class);
        messageContext = Mockito.mock(MessageContext.class);
        samlPeerEntityContext = Mockito.mock(SAMLPeerEntityContext.class);
        ssoRequest = Mockito.mock(SsoRequest.class);
    }

    public SsoRequest getSsoRequest() {
        return ssoRequest;
    }

    public SAMLPeerEntityContext getSAMLPeerEntityContext() {
        return samlPeerEntityContext;
    }

    public MessageContext getMessageContext() {
        return messageContext;
    }

    public WebProviderAuthenticatorHelper getAuthHelper() {
        return authHelper;
    }

    public HttpServletRequest getServletRequest() {
        return servletRequest;
    }

    public HttpServletResponse getServletResponse() {
        return servletResponse;
    }

    public HttpSession getSession() {
        return session;
    }

    public SsoSamlService getSsoService() {
        return ssoService;
    }

    public SsoConfig getSsoConfig() {
        return ssoConfig;
    }

    public Saml20Token getSso20Token() {
        return sso20Token;
    }

    public Cache getCache() {
        return cache;
    }

    public WebAppSecurityConfig getWebAppSecConfig() {
        return webAppSecConfig;
    }

    public Assertion getAssertion() {
        return assertion;
    }

    public Issuer getIssuer() {
        return issuer;
    }

    public EntityDescriptor getEntityDescriptor() {
        return entityDescriptor;
    }

    public KeyInfoCredentialResolver getKeyInfoCredResolver() {
        return keyInfoCredResolver;
    }

    public ForwardRequestInfo getRequestInfo() {
        return requestInfo;
    }

    public Signature getSignature() {
        return signature;
    }

    public Response getSamlResponse() {
        return samlResponse;
    }

    public Status getStatus() {
        return status;
    }

    public StatusCode getStatusCode() {
        return statusCode;
    }

    public Subject getSubject() {
        return subject;
    }

    public SubjectConfirmation getSubjectConfirmation() {
        return subjectConfirmation;
    }

    public SubjectConfirmationData getSubjectConfirmationData() {
        return subjectConfirmationData;
    }

    public Condition getCondition() {
        return condition;
    }

    public Conditions getConditions() {
        return conditions;
    }

    public AuthnStatement getAuthnStatement() {
        return authnStatement;
    }

    public NameID getNameId() {
        return nameId;
    }

    public AudienceRestriction getAudienceRestriction() {
        return audienceRestriction;
    }

    public EncryptedAssertion getEncryptedAssertion() {
        return encryptedAssertion;
    }

    public BasicMessageContextBuilder getBasicMessageContextBuilder() {
        return basicMessageContextBuilder;
    }

    public BasicMessageContext getBasicMessageContext() {
        return basicMessageContext;
    }
}

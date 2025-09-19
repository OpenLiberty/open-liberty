/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.wss4j.common.saml.builder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.bean.ActionBean;
import org.apache.wss4j.common.saml.bean.AdviceBean;
import org.apache.wss4j.common.saml.bean.AttributeBean;
import org.apache.wss4j.common.saml.bean.AttributeStatementBean;
import org.apache.wss4j.common.saml.bean.AudienceRestrictionBean;
import org.apache.wss4j.common.saml.bean.AuthDecisionStatementBean;
import org.apache.wss4j.common.saml.bean.AuthenticationStatementBean;
import org.apache.wss4j.common.saml.bean.ConditionsBean;
import org.apache.wss4j.common.saml.bean.DelegateBean;
import org.apache.wss4j.common.saml.bean.KeyInfoBean;
import org.apache.wss4j.common.saml.bean.NameIDBean;
import org.apache.wss4j.common.saml.bean.ProxyRestrictionBean;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.SubjectConfirmationDataBean;
import org.apache.wss4j.common.saml.bean.SubjectLocalityBean;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSStringBuilder;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.ext.saml2delrestrict.Delegate;
import org.opensaml.saml.ext.saml2delrestrict.DelegationRestrictionType;
import org.opensaml.saml.saml2.core.Action;
import org.opensaml.saml.saml2.core.Advice;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AssertionIDRef;
import org.opensaml.saml.saml2.core.AssertionURIRef;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AttributeValue;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.AuthzDecisionStatement;
import org.opensaml.saml.saml2.core.Conditions;
import org.opensaml.saml.saml2.core.DecisionTypeEnumeration;
import org.opensaml.saml.saml2.core.Evidence;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.KeyInfoConfirmationDataType;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.OneTimeUse;
import org.opensaml.saml.saml2.core.ProxyRestriction;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.SubjectLocality;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.w3c.dom.Element;

/**
 * Class SAML2ComponentBuilder provides builder methods that can be used
 * to construct SAML v2.0 statements using the OpenSaml library.
 */
public final class SAML2ComponentBuilder {

    private static final transient org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(SAML2ComponentBuilder.class);

    private static volatile SAMLObjectBuilder<Assertion> assertionBuilder;

    private static volatile SAMLObjectBuilder<Issuer> issuerBuilder;

    private static volatile SAMLObjectBuilder<Subject> subjectBuilder;

    private static volatile SAMLObjectBuilder<NameID> nameIdBuilder;

    private static volatile SAMLObjectBuilder<SubjectConfirmation> subjectConfirmationBuilder;

    private static volatile SAMLObjectBuilder<OneTimeUse> oneTimeUseBuilder;

    private static volatile SAMLObjectBuilder<ProxyRestriction> proxyRestrictionBuilder;

    private static volatile SAMLObjectBuilder<Conditions> conditionsBuilder;

    private static volatile SAMLObjectBuilder<Advice> adviceBuilder;

    private static volatile SAMLObjectBuilder<AssertionIDRef> assertionIDRefBuilder;

    private static volatile SAMLObjectBuilder<AssertionURIRef> assertionURIRefBuilder;

    private static volatile SAMLObjectBuilder<SubjectConfirmationData> subjectConfirmationDataBuilder;

    private static volatile SAMLObjectBuilder<KeyInfoConfirmationDataType> keyInfoConfirmationDataBuilder;

    private static volatile SAMLObjectBuilder<AuthnStatement> authnStatementBuilder;

    private static volatile SAMLObjectBuilder<AuthnContext> authnContextBuilder;

    private static volatile SAMLObjectBuilder<AuthnContextClassRef> authnContextClassRefBuilder;

    private static volatile SAMLObjectBuilder<AttributeStatement> attributeStatementBuilder;

    private static volatile SAMLObjectBuilder<Attribute> attributeBuilder;

    private static volatile XSStringBuilder stringBuilder;

    private static volatile SAMLObjectBuilder<AudienceRestriction> audienceRestrictionBuilder;

    private static volatile SAMLObjectBuilder<DelegationRestrictionType> delegationRestrictionBuilder;

    private static volatile SAMLObjectBuilder<Audience> audienceBuilder;

    private static volatile SAMLObjectBuilder<Delegate> delegateBuilder;

    private static volatile SAMLObjectBuilder<AuthzDecisionStatement> authorizationDecisionStatementBuilder;

    private static volatile SAMLObjectBuilder<Action> actionElementBuilder;

    private static volatile XMLObjectBuilderFactory builderFactory =
        XMLObjectProviderRegistrySupport.getBuilderFactory();

    private static volatile SAMLObjectBuilder<SubjectLocality> subjectLocalityBuilder;

    private SAML2ComponentBuilder() {
        // Complete
    }

    /**
     * Create a SAML 2 assertion
     *
     * @return a SAML 2 assertion
     */
    @SuppressWarnings("unchecked")
    public static Assertion createAssertion() {
        if (assertionBuilder == null) {
            assertionBuilder = (SAMLObjectBuilder<Assertion>)
                builderFactory.getBuilder(Assertion.DEFAULT_ELEMENT_NAME);
            if (assertionBuilder == null) {
                throw new IllegalStateException(
                    "OpenSaml engine not initialized. Please make sure to initialize the OpenSaml engine "
                    + "prior using it"
                );
            }
        }
        Assertion assertion =
            assertionBuilder.buildObject(Assertion.DEFAULT_ELEMENT_NAME, Assertion.TYPE_NAME);
        assertion.setID(IDGenerator.generateID("_"));
        assertion.setVersion(SAMLVersion.VERSION_20);
        assertion.setIssueInstant(Instant.now());
        return assertion;
    }

    /**
     * Create an Issuer object
     *
     * @param issuerValue of type String
     * @param issuerFormat of type String
     * @param issuerQualifier of type String
     * @return an Issuer object
     */
    @SuppressWarnings("unchecked")
    public static Issuer createIssuer(String issuerValue, String issuerFormat, String issuerQualifier) {
        if (issuerBuilder == null) {
            issuerBuilder = (SAMLObjectBuilder<Issuer>)
                builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);

        }
        Issuer issuer = issuerBuilder.buildObject();
        //
        // The SAML authority that is making the claim(s) in the assertion. The issuer SHOULD
        // be unambiguous to the intended relying parties.
        issuer.setValue(issuerValue);
        issuer.setFormat(issuerFormat);
        issuer.setNameQualifier(issuerQualifier);
        return issuer;
    }

    /**
     * Create a Conditions object
     *
     * @param conditionsBean A ConditionsBean object
     * @return a Conditions object
     */
    @SuppressWarnings("unchecked")
    public static Conditions createConditions(ConditionsBean conditionsBean) {
        if (conditionsBuilder == null) {
            conditionsBuilder = (SAMLObjectBuilder<Conditions>)
                builderFactory.getBuilder(Conditions.DEFAULT_ELEMENT_NAME);
        }

        Conditions conditions = conditionsBuilder.buildObject();

        if (conditionsBean == null) {
            Instant newNotBefore = Instant.now();
            conditions.setNotBefore(newNotBefore);
            conditions.setNotOnOrAfter(newNotBefore.plus(Duration.ofMinutes(5)));
            return conditions;
        }

        long tokenPeriodSeconds = conditionsBean.getTokenPeriodSeconds();
        Instant notBefore = conditionsBean.getNotBefore();
        Instant notAfter = conditionsBean.getNotAfter();

        if (notBefore != null && notAfter != null) {
            if (notBefore.isAfter(notAfter)) {
                throw new IllegalStateException(
                    "The value of notBefore may not be after the value of notAfter"
                );
            }
            conditions.setNotBefore(notBefore);
            conditions.setNotOnOrAfter(notAfter);
        } else {
            Instant newNotBefore = Instant.now();
            conditions.setNotBefore(newNotBefore);
            if (tokenPeriodSeconds <= 0) {
                tokenPeriodSeconds = 5L * 60L;
            }
            Instant notOnOrAfter = newNotBefore.plusSeconds(tokenPeriodSeconds);
            conditions.setNotOnOrAfter(notOnOrAfter);
        }

        if (conditionsBean.getAudienceRestrictions() != null
            && !conditionsBean.getAudienceRestrictions().isEmpty()) {
            for (AudienceRestrictionBean audienceRestrictionBean
                : conditionsBean.getAudienceRestrictions()) {
                AudienceRestriction audienceRestriction =
                        createAudienceRestriction(audienceRestrictionBean);
                conditions.getAudienceRestrictions().add(audienceRestriction);
            }
        }

        if (conditionsBean.isOneTimeUse()) {
            conditions.getConditions().add(createOneTimeUse());
        }

        if (conditionsBean.getProxyRestriction() != null) {
            conditions.getConditions().add(createProxyRestriction(conditionsBean.getProxyRestriction()));
        }

        if (conditionsBean.getDelegates() != null && !conditionsBean.getDelegates().isEmpty()) {
            DelegationRestrictionType delegationRestriction =
                createDelegationRestriction(conditionsBean.getDelegates());
            conditions.getConditions().add(delegationRestriction);
        }

        return conditions;
    }

    /**
     * Create a Advice object
     *
     * @param adviceBean A AdviceBean object
     * @return a Advice object
     * @throws WSSecurityException
     */
    @SuppressWarnings("unchecked")
    public static Advice createAdvice(AdviceBean adviceBean) throws WSSecurityException {
        if (adviceBuilder == null) {
            adviceBuilder = (SAMLObjectBuilder<Advice>)
                builderFactory.getBuilder(Advice.DEFAULT_ELEMENT_NAME);
        }

        Advice advice = adviceBuilder.buildObject();

        if (!adviceBean.getIdReferences().isEmpty()) {
            if (assertionIDRefBuilder == null) {
                assertionIDRefBuilder = (SAMLObjectBuilder<AssertionIDRef>)
                    builderFactory.getBuilder(AssertionIDRef.DEFAULT_ELEMENT_NAME);
            }

            for (String ref : adviceBean.getIdReferences()) {
                AssertionIDRef assertionIdRef = assertionIDRefBuilder.buildObject();
                assertionIdRef.setValue(ref);
                advice.getAssertionIDReferences().add(assertionIdRef);
            }
        }

        if (!adviceBean.getUriReferences().isEmpty()) {
            if (assertionURIRefBuilder == null) {
                assertionURIRefBuilder = (SAMLObjectBuilder<AssertionURIRef>)
                    builderFactory.getBuilder(AssertionURIRef.DEFAULT_ELEMENT_NAME);
            }

            for (String ref : adviceBean.getUriReferences()) {
                AssertionURIRef assertionURIRef = assertionURIRefBuilder.buildObject();
                assertionURIRef.setURI(ref);
                advice.getAssertionURIReferences().add(assertionURIRef);
            }
        }

        if (!adviceBean.getAssertions().isEmpty()) {
            for (Element assertionElement : adviceBean.getAssertions()) {
                XMLObject xmlObject = OpenSAMLUtil.fromDom(assertionElement);
                if (xmlObject instanceof Assertion) {
                    Assertion assertion = (Assertion)xmlObject;
                    advice.getAssertions().add(assertion);
                }
            }
        }

        return advice;
    }

    /**
     * Create an AudienceRestriction object
     *
     * @param audienceRestrictionBean of type AudienceRestrictionBean
     * @return an AudienceRestriction object
     */
    @SuppressWarnings("unchecked")
    public static AudienceRestriction createAudienceRestriction(
        AudienceRestrictionBean audienceRestrictionBean
    ) {
        if (audienceRestrictionBuilder == null) {
            audienceRestrictionBuilder = (SAMLObjectBuilder<AudienceRestriction>)
                builderFactory.getBuilder(AudienceRestriction.DEFAULT_ELEMENT_NAME);
        }
        if (audienceBuilder == null) {
            audienceBuilder = (SAMLObjectBuilder<Audience>)
                builderFactory.getBuilder(Audience.DEFAULT_ELEMENT_NAME);
        }

        AudienceRestriction audienceRestriction = audienceRestrictionBuilder.buildObject();

        for (String audienceURI : audienceRestrictionBean.getAudienceURIs()) {
            Audience audience = audienceBuilder.buildObject();
            audience.setURI(audienceURI);
            audienceRestriction.getAudiences().add(audience);
        }
        return audienceRestriction;
    }

    /**
     * Create an DelegationRestrictionType object
     *
     * @param delegates of type List<DelegateBean>
     * @return a DelegationRestrictionType object
     */
    @SuppressWarnings("unchecked")
    public static DelegationRestrictionType createDelegationRestriction(
        List<DelegateBean> delegates
    ) {
        if (delegationRestrictionBuilder == null) {
            delegationRestrictionBuilder = (SAMLObjectBuilder<DelegationRestrictionType>)
                builderFactory.getBuilder(DelegationRestrictionType.TYPE_NAME);
        }
        DelegationRestrictionType delegationRestriction = delegationRestrictionBuilder.buildObject();

        if (delegateBuilder == null) {
            delegateBuilder = (SAMLObjectBuilder<Delegate>)
                builderFactory.getBuilder(Delegate.DEFAULT_ELEMENT_NAME);
        }

        for (DelegateBean delegateBean : delegates) {
            Delegate delegate = delegateBuilder.buildObject();
            delegate.setConfirmationMethod(delegateBean.getConfirmationMethod());
            delegate.setDelegationInstant(delegateBean.getDelegationInstant());

            if (delegateBean.getNameIDBean() == null) {
                throw new IllegalStateException(
                   "The value of NameIDBean in DelegateBean may not be null"
                );
            }
            NameID nameID = createNameID(delegateBean.getNameIDBean());
            delegate.setNameID(nameID);
            delegationRestriction.getDelegates().add(delegate);
        }

        return delegationRestriction;
    }

    /**
     * Create a OneTimeUse object
     *
     * @return a OneTimeUse object
     */
    @SuppressWarnings("unchecked")
    public static OneTimeUse createOneTimeUse() {
        if (oneTimeUseBuilder == null) {
            oneTimeUseBuilder = (SAMLObjectBuilder<OneTimeUse>)
                builderFactory.getBuilder(OneTimeUse.DEFAULT_ELEMENT_NAME);
        }

        return oneTimeUseBuilder.buildObject();
    }

    /**
     * Create a ProxyRestriction object
     *
     * @return a ProxyRestriction object
     */
    @SuppressWarnings("unchecked")
    public static ProxyRestriction createProxyRestriction(ProxyRestrictionBean proxyRestrictionBean) {
        if (proxyRestrictionBuilder == null) {
            proxyRestrictionBuilder = (SAMLObjectBuilder<ProxyRestriction>)
                builderFactory.getBuilder(ProxyRestriction.DEFAULT_ELEMENT_NAME);
        }

        ProxyRestriction proxyRestriction = proxyRestrictionBuilder.buildObject();
        if (proxyRestrictionBean.getCount() > 0) {
            proxyRestriction.setProxyCount(proxyRestrictionBean.getCount());
        }

        if (!proxyRestrictionBean.getAudienceURIs().isEmpty()) {
            if (audienceBuilder == null) {
                audienceBuilder = (SAMLObjectBuilder<Audience>)
                    builderFactory.getBuilder(Audience.DEFAULT_ELEMENT_NAME);
            }
            for (String audienceURI : proxyRestrictionBean.getAudienceURIs()) {
                Audience audience = audienceBuilder.buildObject();
                audience.setURI(audienceURI);
                proxyRestriction.getAudiences().add(audience);
            }
        }

        return proxyRestriction;
    }

    /**
     * Create SAML 2 Authentication Statement(s).
     *
     * @param authBeans A list of AuthenticationStatementBean instances
     * @return SAML 2 Authentication Statement(s).
     */
    @SuppressWarnings("unchecked")
    public static List<AuthnStatement> createAuthnStatement(
        List<AuthenticationStatementBean> authBeans
    ) {
        List<AuthnStatement> authnStatements = new ArrayList<>();

        if (authnStatementBuilder == null) {
            authnStatementBuilder = (SAMLObjectBuilder<AuthnStatement>)
                builderFactory.getBuilder(AuthnStatement.DEFAULT_ELEMENT_NAME);
        }
        if (authnContextBuilder == null) {
            authnContextBuilder = (SAMLObjectBuilder<AuthnContext>)
                builderFactory.getBuilder(AuthnContext.DEFAULT_ELEMENT_NAME);
        }
        if (authnContextClassRefBuilder == null) {
            authnContextClassRefBuilder = (SAMLObjectBuilder<AuthnContextClassRef>)
                builderFactory.getBuilder(AuthnContextClassRef.DEFAULT_ELEMENT_NAME);
        }
        if (subjectLocalityBuilder == null) {
            subjectLocalityBuilder = (SAMLObjectBuilder<SubjectLocality>)
            builderFactory.getBuilder(SubjectLocality.DEFAULT_ELEMENT_NAME);
        }

        if (authBeans != null && !authBeans.isEmpty()) {
            for (AuthenticationStatementBean statementBean : authBeans) {
                AuthnStatement authnStatement = authnStatementBuilder.buildObject();
                Instant authInstant = statementBean.getAuthenticationInstant();
                if (authInstant == null) {
                    authInstant = Instant.now();
                }
                authnStatement.setAuthnInstant(authInstant);

                Instant sessionNotOnOrAfter = statementBean.getSessionNotOnOrAfter();
                if (sessionNotOnOrAfter != null) {
                    authnStatement.setSessionNotOnOrAfter(sessionNotOnOrAfter);
                }

                if (statementBean.getSessionIndex() != null) {
                    authnStatement.setSessionIndex(statementBean.getSessionIndex());
                }

                AuthnContextClassRef authnContextClassRef = authnContextClassRefBuilder.buildObject();
                authnContextClassRef.setURI(
                    transformAuthenticationMethod(statementBean.getAuthenticationMethod())
                );
                AuthnContext authnContext = authnContextBuilder.buildObject();
                authnContext.setAuthnContextClassRef(authnContextClassRef);
                authnStatement.setAuthnContext(authnContext);

                SubjectLocalityBean subjectLocalityBean = statementBean.getSubjectLocality();
                if (subjectLocalityBean != null) {
                    SubjectLocality subjectLocality = subjectLocalityBuilder.buildObject();
                    subjectLocality.setDNSName(subjectLocalityBean.getDnsAddress());
                    subjectLocality.setAddress(subjectLocalityBean.getIpAddress());

                    authnStatement.setSubjectLocality(subjectLocality);
                }

                authnStatements.add(authnStatement);
            }
        }

        return authnStatements;
    }

    /**
     * Transform the user-supplied authentication method value into one of the supported
     * specification-compliant values.
     *
     * @param sourceMethod of type String
     * @return String
     */
    private static String transformAuthenticationMethod(String sourceMethod) {
        String transformedMethod = "";

        if ("Password".equalsIgnoreCase(sourceMethod)) {
            transformedMethod = SAML2Constants.AUTH_CONTEXT_CLASS_REF_PASSWORD;
        } else if (sourceMethod != null && sourceMethod.length() != 0) {
            return sourceMethod;
        }

        return transformedMethod;
    }

    /**
     * Create a SAML2 Attribute
     *
     * @param friendlyName of type String
     * @param name         of type String
     * @param nameFormat   of type String
     * @param values       of type ArrayList
     * @return a SAML2 Attribute
     */
    public static Attribute createAttribute(
        String friendlyName, String name, String nameFormat, List<Object> values
    ) {
        if (stringBuilder == null) {
            stringBuilder = (XSStringBuilder)builderFactory.getBuilder(XSString.TYPE_NAME);
        }
        Attribute attribute = createAttribute(friendlyName, name, nameFormat);

        for (Object value : values) {
            if (value instanceof String) {
                XSString attributeValue =
                    stringBuilder.buildObject(AttributeValue.DEFAULT_ELEMENT_NAME, XSString.TYPE_NAME);
                attributeValue.setValue((String)value);
                attribute.getAttributeValues().add(attributeValue);
            } else if (value instanceof XMLObject) {
                attribute.getAttributeValues().add((XMLObject)value);
            }
        }

        return attribute;
    }

    /**
     * Create a Subject.
     *
     * @param subjectBean of type SubjectBean
     * @return a Subject
     */
    @SuppressWarnings("unchecked")
    public static Subject createSaml2Subject(SubjectBean subjectBean)
        throws org.opensaml.security.SecurityException, WSSecurityException {
        if (subjectBuilder == null) {
            subjectBuilder = (SAMLObjectBuilder<Subject>)
                builderFactory.getBuilder(Subject.DEFAULT_ELEMENT_NAME);
        }
        Subject subject = subjectBuilder.buildObject();

        NameID nameID = SAML2ComponentBuilder.createNameID(subjectBean);
        subject.setNameID(nameID);

        SubjectConfirmationData subjectConfData = null;
        if (subjectBean.getKeyInfo() != null || subjectBean.getSubjectConfirmationData() != null) {
            subjectConfData =
                SAML2ComponentBuilder.createSubjectConfirmationData(
                    subjectBean.getSubjectConfirmationData(),
                    subjectBean.getKeyInfo()
                );
        }

        NameID subjectConfNameId = null;
        if (subjectBean.getSubjectConfirmationNameID() != null) {
            subjectConfNameId = SAML2ComponentBuilder.createNameID(subjectBean.getSubjectConfirmationNameID());
        }

        String confirmationMethodStr = subjectBean.getSubjectConfirmationMethod();
        if (confirmationMethodStr == null) {
            confirmationMethodStr = SAML2Constants.CONF_SENDER_VOUCHES;
        }
        SubjectConfirmation subjectConfirmation =
            SAML2ComponentBuilder.createSubjectConfirmation(
                confirmationMethodStr, subjectConfData, subjectConfNameId
            );

        subject.getSubjectConfirmations().add(subjectConfirmation);
        return subject;
    }

    /**
     * Create a SubjectConfirmationData object
     *
     * @param subjectConfirmationDataBean of type SubjectConfirmationDataBean
     * @param keyInfoBean of type KeyInfoBean
     * @return a SubjectConfirmationData object
     */
    @SuppressWarnings("unchecked")
    public static SubjectConfirmationData createSubjectConfirmationData(
        SubjectConfirmationDataBean subjectConfirmationDataBean,
        KeyInfoBean keyInfoBean
    ) throws org.opensaml.security.SecurityException, WSSecurityException {
        SubjectConfirmationData subjectConfirmationData = null;
        KeyInfo keyInfo = null;
        if (keyInfoBean == null) {
            if (subjectConfirmationDataBuilder == null) {
                subjectConfirmationDataBuilder = (SAMLObjectBuilder<SubjectConfirmationData>)
                    builderFactory.getBuilder(SubjectConfirmationData.DEFAULT_ELEMENT_NAME);
            }
            subjectConfirmationData = subjectConfirmationDataBuilder.buildObject();
        } else {
            if (keyInfoConfirmationDataBuilder == null) {
                keyInfoConfirmationDataBuilder = (SAMLObjectBuilder<KeyInfoConfirmationDataType>)
                    builderFactory.getBuilder(KeyInfoConfirmationDataType.TYPE_NAME);
            }
            subjectConfirmationData = keyInfoConfirmationDataBuilder.buildObject();
            keyInfo = SAML1ComponentBuilder.createKeyInfo(keyInfoBean);
            ((KeyInfoConfirmationDataType)subjectConfirmationData).getKeyInfos().add(keyInfo);
        }

        if (subjectConfirmationDataBean != null) {
            if (subjectConfirmationDataBean.getInResponseTo() != null) {
                subjectConfirmationData.setInResponseTo(subjectConfirmationDataBean.getInResponseTo());
            }
            if (subjectConfirmationDataBean.getRecipient() != null) {
                subjectConfirmationData.setRecipient(subjectConfirmationDataBean.getRecipient());
            }
            if (subjectConfirmationDataBean.getAddress() != null) {
                subjectConfirmationData.setAddress(subjectConfirmationDataBean.getAddress());
            }
            if (subjectConfirmationDataBean.getNotAfter() != null) {
                subjectConfirmationData.setNotOnOrAfter(subjectConfirmationDataBean.getNotAfter());
            }
            if (subjectConfirmationDataBean.getNotBefore() != null) {
                subjectConfirmationData.setNotBefore(subjectConfirmationDataBean.getNotBefore());
            }
            if (subjectConfirmationDataBean.getAny() != null) {
                List<XMLObject> unknownObjects = subjectConfirmationData.getUnknownXMLObjects();
                for (Object obj : subjectConfirmationDataBean.getAny()) {
                    if (obj == null) {
                        LOG.warn("Ignore <null> object in SubjectConfirmationData.any");
                    } else if (obj instanceof XMLObject) {
                        unknownObjects.add((XMLObject) obj);
                    } else if (obj instanceof AttributeStatementBean) {
                        unknownObjects.addAll(createAttributeStatement(Collections.singletonList((AttributeStatementBean) obj)));
                    } else {
                        LOG.warn("Ignore object of the unsupported type {} in SubjectConfirmationData.any", obj.getClass());
                    }
                }
            }
        }

        return subjectConfirmationData;
    }

    /**
     * Create a SubjectConfirmation object
     * One of the following subject confirmation methods MUST be used:
     *   urn:oasis:names:tc:SAML:2.0:cm:holder-of-key
     *   urn:oasis:names:tc:SAML:2.0:cm:sender-vouches
     *   urn:oasis:names:tc:SAML:2.0:cm:bearer
     *
     * @param method of type String
     * @param subjectConfirmationData of type SubjectConfirmationData
     * @return a SubjectConfirmation object
     */
    @SuppressWarnings("unchecked")
    public static SubjectConfirmation createSubjectConfirmation(
        String method,
        SubjectConfirmationData subjectConfirmationData,
        NameID subjectConfirmationNameId
    ) {
        if (subjectConfirmationBuilder == null) {
            subjectConfirmationBuilder = (SAMLObjectBuilder<SubjectConfirmation>)
                builderFactory.getBuilder(SubjectConfirmation.DEFAULT_ELEMENT_NAME);
        }

        SubjectConfirmation subjectConfirmation = subjectConfirmationBuilder.buildObject();
        subjectConfirmation.setMethod(method);
        subjectConfirmation.setSubjectConfirmationData(subjectConfirmationData);
        subjectConfirmation.setNameID(subjectConfirmationNameId);
        return subjectConfirmation;
    }

    /**
     * Create a NameID object
     * One of the following formats MUST be used:
     *   urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified
     *   urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress
     *   urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName
     *   urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName
     *   urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos
     *   urn:oasis:names:tc:SAML:2.0:nameid-format:entity
     *   urn:oasis:names:tc:SAML:2.0:nameid-format:persistent
     *   urn:oasis:names:tc:SAML:2.0:nameid-format:transient
     *
     * @param subject A SubjectBean instance
     * @return NameID
     */
    public static NameID createNameID(SubjectBean subject) {
        NameIDBean nameIDBean = new NameIDBean();
        nameIDBean.setNameIDFormat(subject.getSubjectNameIDFormat());
        nameIDBean.setNameQualifier(subject.getSubjectNameQualifier());
        nameIDBean.setSPNameQualifier(subject.getSubjectNameSPNameQualifier());
        nameIDBean.setSPProvidedID(subject.getSubjectNameSPProvidedID());
        nameIDBean.setNameValue(subject.getSubjectName());
        return createNameID(nameIDBean);
    }

    @SuppressWarnings("unchecked")
    public static NameID createNameID(NameIDBean nameIDBean) {
        if (nameIdBuilder == null) {
            nameIdBuilder = (SAMLObjectBuilder<NameID>)
                builderFactory.getBuilder(NameID.DEFAULT_ELEMENT_NAME);
        }
        NameID nameID = nameIdBuilder.buildObject();
        nameID.setNameQualifier(nameIDBean.getNameQualifier());
        nameID.setFormat(nameIDBean.getNameIDFormat());
        nameID.setValue(nameIDBean.getNameValue());
        nameID.setSPNameQualifier(nameIDBean.getSPNameQualifier());
        nameID.setSPProvidedID(nameIDBean.getSPProvidedID());
        return nameID;
    }

    /**
     * Create SAML2 Attribute Statement(s)
     *
     * @param attributeData A list of AttributeStatementBean instances
     * @return SAML2 Attribute Statement(s)
     */
    @SuppressWarnings("unchecked")
    public static List<AttributeStatement> createAttributeStatement(
        List<AttributeStatementBean> attributeData
    ) {
        List<AttributeStatement> attributeStatements = new ArrayList<>();
        if (attributeStatementBuilder == null) {
            attributeStatementBuilder = (SAMLObjectBuilder<AttributeStatement>)
            builderFactory.getBuilder(AttributeStatement.DEFAULT_ELEMENT_NAME);
        }

        if (attributeData != null && !attributeData.isEmpty()) {
            for (AttributeStatementBean statementBean : attributeData) {
                AttributeStatement attributeStatement = attributeStatementBuilder.buildObject();
                for (AttributeBean values : statementBean.getSamlAttributes()) {
                    List<Object> attributeValues = values.getAttributeValues();
                    Attribute samlAttribute =
                        createAttribute(
                            values.getSimpleName(),
                            values.getQualifiedName(),
                            values.getNameFormat(),
                            attributeValues
                        );
                    attributeStatement.getAttributes().add(samlAttribute);
                }
                // Add the completed attribute statementBean to the collection
                attributeStatements.add(attributeStatement);
            }
        }

        return attributeStatements;
    }

    /**
     * Create an Attribute object.
     *
     * @param friendlyName of type String
     * @param name of type String
     * @param nameFormat of type String
     * @return an Attribute object
     */
    @SuppressWarnings("unchecked")
    public static Attribute createAttribute(String friendlyName, String name, String nameFormat) {
        if (attributeBuilder == null) {
            attributeBuilder = (SAMLObjectBuilder<Attribute>)
                builderFactory.getBuilder(Attribute.DEFAULT_ELEMENT_NAME);
        }

        Attribute attribute = attributeBuilder.buildObject();
        attribute.setFriendlyName(friendlyName);
        if (nameFormat == null) {
            attribute.setNameFormat(SAML2Constants.ATTRNAME_FORMAT_URI);
        } else {
            attribute.setNameFormat(nameFormat);
        }
        attribute.setName(name);
        return attribute;
    }

    /**
     * Create SAML2 AuthorizationDecisionStatement(s)
     *
     * @param decisionData A list of AuthDecisionStatementBean instances
     * @return SAML2 AuthorizationDecisionStatement(s)
     */
    @SuppressWarnings("unchecked")
    public static List<AuthzDecisionStatement> createAuthorizationDecisionStatement(
        List<AuthDecisionStatementBean> decisionData
    ) {
        List<AuthzDecisionStatement> authDecisionStatements = new ArrayList<>();
        if (authorizationDecisionStatementBuilder == null) {
            authorizationDecisionStatementBuilder =
                (SAMLObjectBuilder<AuthzDecisionStatement>)
                    builderFactory.getBuilder(AuthzDecisionStatement.DEFAULT_ELEMENT_NAME);
        }

        if (decisionData != null && !decisionData.isEmpty()) {
            for (AuthDecisionStatementBean decisionStatementBean : decisionData) {
                AuthzDecisionStatement authDecision =
                    authorizationDecisionStatementBuilder.buildObject();
                authDecision.setResource(decisionStatementBean.getResource());
                authDecision.setDecision(
                    transformDecisionType(decisionStatementBean.getDecision())
                );

                for (ActionBean actionBean : decisionStatementBean.getActions()) {
                    Action actionElement = createSamlAction(actionBean);
                    authDecision.getActions().add(actionElement);
                }

                if (decisionStatementBean.getEvidence() instanceof Evidence) {
                    authDecision.setEvidence((Evidence)decisionStatementBean.getEvidence());
                }

                authDecisionStatements.add(authDecision);
            }
        }

        return authDecisionStatements;
    }


    /**
     * Create an Action object
     *
     * @param actionBean An ActionBean instance
     * @return an Action object
     */
    @SuppressWarnings("unchecked")
    public static Action createSamlAction(ActionBean actionBean) {
        if (actionElementBuilder == null) {
            actionElementBuilder = (SAMLObjectBuilder<Action>)
                builderFactory.getBuilder(Action.DEFAULT_ELEMENT_NAME);
        }
        Action actionElement = actionElementBuilder.buildObject();
        actionElement.setNamespace(actionBean.getActionNamespace());
        if (actionBean.getActionNamespace() == null) {
            actionElement.setNamespace("urn:oasis:names:tc:SAML:1.0:action:rwedc-negation");
        }
        actionElement.setValue(actionBean.getContents());

        return actionElement;
    }

    /**
     * Create a DecisionTypeEnumeration object
     *
     * @param decision of type Decision
     * @return a DecisionTypeEnumeration object
     */
    private static DecisionTypeEnumeration transformDecisionType(
        AuthDecisionStatementBean.Decision decision
    ) {
        DecisionTypeEnumeration decisionTypeEnum = DecisionTypeEnumeration.DENY;
        if (decision.equals(AuthDecisionStatementBean.Decision.PERMIT)) {
            decisionTypeEnum = DecisionTypeEnumeration.PERMIT;
        } else if (decision.equals(AuthDecisionStatementBean.Decision.INDETERMINATE)) {
            decisionTypeEnum = DecisionTypeEnumeration.INDETERMINATE;
        }

        return decisionTypeEnum;
    }

}

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

package org.apache.wss4j.common.saml;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.builder.SAML1ComponentBuilder;
import org.apache.wss4j.common.saml.builder.SAML2ComponentBuilder;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.common.util.InetAddressUtils;
import org.apache.xml.security.stax.impl.util.IDGenerator;
import org.apache.xml.security.utils.XMLUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml1.core.AttributeStatement;
import org.opensaml.saml.saml1.core.AuthenticationStatement;
import org.opensaml.saml.saml1.core.AuthorizationDecisionStatement;
import org.opensaml.saml.saml1.core.ConfirmationMethod;
import org.opensaml.saml.saml1.core.Statement;
import org.opensaml.saml.saml1.core.Subject;
import org.opensaml.saml.saml1.core.SubjectConfirmation;
import org.opensaml.saml.saml1.core.SubjectStatement;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.opensaml.saml.saml2.core.AuthzDecisionStatement;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.credential.BasicCredential;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.SignerProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class SamlAssertionWrapper can generate, sign, and validate both SAML v1.1
 * and SAML v2.0 assertions.
 */
public class SamlAssertionWrapper {
    /**
     * Field LOG
     */
    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(SamlAssertionWrapper.class);

    /**
     * Raw SAML Object
     */
    private SAMLObject samlObject;

    /**
     * Which SAML specification to use (currently, only v1.1 and v2.0 are supported)
     */
    private SAMLVersion samlVersion;

    /**
     * The Assertion as a DOM element
     */
    private Element assertionElement;

    /**
     * The SAMLKeyInfo object associated with the Subject KeyInfo
     */
    private SAMLKeyInfo subjectKeyInfo;

    /**
     * The SAMLKeyInfo object associated with the Signature on the Assertion
     */
    private SAMLKeyInfo signatureKeyInfo;

    /**
     * Default Canonicalization algorithm used for signing.
     */
    private final String defaultCanonicalizationAlgorithm = SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS;

    /**
     * Default RSA Signature algorithm used for signing.
     */
    private final String defaultRSASignatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1;

    /**
     * Default DSA Signature algorithm used for signing.
     */
    private final String defaultDSASignatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_DSA;

    /**
     * Default ECDSA Signature algorithm used for signing.
     */
    private final String defaultECDSASignatureAlgorithm = SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA1;

    /**
     * Default Signature Digest algorithm
     */
    private final String defaultSignatureDigestAlgorithm = SignatureConstants.ALGO_ID_DIGEST_SHA1;

    /**
     * Whether this object was instantiated with a DOM Element or an XMLObject initially
     */
    private final boolean fromDOM;

    /**
     * Constructor SamlAssertionWrapper creates a new SamlAssertionWrapper instance.
     *
     * @param element of type Element
     * @throws WSSecurityException
     */
    public SamlAssertionWrapper(Element element) throws WSSecurityException {
        OpenSAMLUtil.initSamlEngine();

        parseElement(element);
        fromDOM = true;
    }

    /**
     * Constructor SamlAssertionWrapper creates a new SamlAssertionWrapper instance.
     * This is the primary constructor. All other constructor calls should
     * be routed to this method to ensure that the wrapper is initialized
     * correctly.
     *
     * @param samlObject of type SAMLObject
     */
    public SamlAssertionWrapper(SAMLObject samlObject) throws WSSecurityException {
        OpenSAMLUtil.initSamlEngine();

        this.samlObject = samlObject;
        if (samlObject instanceof org.opensaml.saml.saml1.core.Assertion) {
            samlVersion = SAMLVersion.VERSION_11;
        } else if (samlObject instanceof org.opensaml.saml.saml2.core.Assertion) {
            samlVersion = SAMLVersion.VERSION_20;
        } else {
            LOG.error(
                "SamlAssertionWrapper: found unexpected type "
                + (samlObject != null ? samlObject.getClass().getName() : null)
            );
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                                          new Object[] {"A SAML 2.0 or 1.1 Assertion can only be used with "
                                                        + "SamlAssertionWrapper"});
        }
        fromDOM = false;
    }

    /**
     * Constructor SamlAssertionWrapper creates a new SamlAssertionWrapper instance.
     * This constructor is primarily called on the client side to initialize
     * the wrapper from a configuration file. <br>
     *
     * @param samlCallback of type SAMLCallback
     */
    public SamlAssertionWrapper(SAMLCallback samlCallback) throws WSSecurityException {
        OpenSAMLUtil.initSamlEngine();

        if (samlCallback.getAssertionElement() != null) {
            parseElement(samlCallback.getAssertionElement());
            fromDOM = true;
        } else {
            // If not then parse the SAMLCallback object
            parseCallback(samlCallback);
            fromDOM = false;
        }
    }

    /**
     * Method getSaml1 returns the saml1 of this SamlAssertionWrapper object.
     *
     * @return the saml1 (type Assertion) of this SamlAssertionWrapper object.
     */
    public org.opensaml.saml.saml1.core.Assertion getSaml1() {
        if (samlVersion == SAMLVersion.VERSION_11) {
            return (org.opensaml.saml.saml1.core.Assertion)samlObject;
        }
        return null;
    }

    /**
     * Method getSaml2 returns the saml2 of this SamlAssertionWrapper object.
     *
     * @return the saml2 (type Assertion) of this SamlAssertionWrapper object.
     */
    public org.opensaml.saml.saml2.core.Assertion getSaml2() {
        if (samlVersion == SAMLVersion.VERSION_20) {
            return (org.opensaml.saml.saml2.core.Assertion)samlObject;
        }
        return null;
    }

    /**
     * Method isCreated returns the created of this SamlAssertionWrapper object.
     *
     * @return the created (type boolean) of this SamlAssertionWrapper object.
     */
    public boolean isCreated() {
        return samlObject != null;
    }


    /**
     * Create a DOM from the current XMLObject content. If the user-supplied doc is not null,
     * reparent the returned Element so that it is compatible with the user-supplied document.
     *
     * @param doc of type Document
     * @return Element
     */
    public Element toDOM(Document doc) throws WSSecurityException {
        if (fromDOM && assertionElement != null) {
            parseElement(assertionElement);
            if (doc != null) {
                return (Element)doc.importNode(assertionElement, true);
            }
            return assertionElement;
        }
        assertionElement = OpenSAMLUtil.toDom(samlObject, doc);
        return assertionElement;
    }

    /**
     * Method assertionToString ...
     *
     * @return String
     */
    public String assertionToString() throws WSSecurityException {
        if (assertionElement == null) {
            Element element = toDOM(null);
            return DOM2Writer.nodeToString(element);
        }
        return DOM2Writer.nodeToString(assertionElement);
    }

    public Instant getNotBefore() {
        if (getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            return getSaml2().getConditions().getNotBefore();
        } else {
            return getSaml1().getConditions().getNotBefore();
        }
    }

    public Instant getNotOnOrAfter() {
        if (getSamlVersion().equals(SAMLVersion.VERSION_20)) {
            return getSaml2().getConditions().getNotOnOrAfter();
        } else {
            return getSaml1().getConditions().getNotOnOrAfter();
        }
    }

    /**
     * Method getId returns the id of this SamlAssertionWrapper object.
     *
     * @return the id (type String) of this SamlAssertionWrapper object.
     */
    public String getId() {
        String id = null;
        if (samlVersion == SAMLVersion.VERSION_20) {
            id = ((org.opensaml.saml.saml2.core.Assertion)samlObject).getID();
            if (id == null || id.length() == 0) {
                LOG.error("SamlAssertionWrapper: ID was null, seeting a new ID value");
                id = IDGenerator.generateID("_");
                ((org.opensaml.saml.saml2.core.Assertion)samlObject).setID(id);
            }
        } else if (samlVersion == SAMLVersion.VERSION_11) {
            id = ((org.opensaml.saml.saml1.core.Assertion)samlObject).getID();
            if (id == null || id.length() == 0) {
                LOG.error("SamlAssertionWrapper: ID was null, seeting a new ID value");
                id = IDGenerator.generateID("_");
                ((org.opensaml.saml.saml1.core.Assertion)samlObject).setID(id);
            }
        } else {
            LOG.error("SamlAssertionWrapper: unable to return ID - no saml assertion object");
        }
        return id;
    }

    /**
     * Method getIssuerString returns the issuerString of this SamlAssertionWrapper object.
     *
     * @return the issuerString (type String) of this SamlAssertionWrapper object.
     */
    public String getIssuerString() {
        if (samlVersion == SAMLVersion.VERSION_20
            && ((org.opensaml.saml.saml2.core.Assertion)samlObject).getIssuer() != null) {
            return ((org.opensaml.saml.saml2.core.Assertion)samlObject).getIssuer().getValue();
        } else if (samlVersion == SAMLVersion.VERSION_11
            && ((org.opensaml.saml.saml1.core.Assertion)samlObject).getIssuer() != null) {
            return ((org.opensaml.saml.saml1.core.Assertion)samlObject).getIssuer();
        }
        LOG.error(
            "SamlAssertionWrapper: unable to return Issuer string - no saml assertion "
            + "object or issuer is null"
        );
        return null;
    }

    /**
     * Method getSubjectName returns the Subject name value
     * @return the subjectName of this SamlAssertionWrapper object
     */
    public String getSubjectName() {
        if (samlVersion == SAMLVersion.VERSION_20) {
            org.opensaml.saml.saml2.core.Subject subject =
                ((org.opensaml.saml.saml2.core.Assertion)samlObject).getSubject();
            if (subject != null && subject.getNameID() != null) {
                return subject.getNameID().getValue();
            }
        } else if (samlVersion == SAMLVersion.VERSION_11) {
            Subject samlSubject = null;
            for (Statement stmt : ((org.opensaml.saml.saml1.core.Assertion)samlObject).getStatements()) {
                if (stmt instanceof AttributeStatement) {
                    AttributeStatement attrStmt = (AttributeStatement) stmt;
                    samlSubject = attrStmt.getSubject();
                } else if (stmt instanceof AuthenticationStatement) {
                    AuthenticationStatement authStmt = (AuthenticationStatement) stmt;
                    samlSubject = authStmt.getSubject();
                } else {
                    AuthorizationDecisionStatement authzStmt =
                        (AuthorizationDecisionStatement)stmt;
                    samlSubject = authzStmt.getSubject();
                }
                if (samlSubject != null) {
                    break;
                }
            }
            if (samlSubject != null && samlSubject.getNameIdentifier() != null) {
                return samlSubject.getNameIdentifier().getValue();
            }
        }
        LOG.error(
                "SamlAssertionWrapper: unable to return SubjectName - no saml assertion "
                        + "object or subject is null"
        );
        return null;
    }

    /**
     * Method getConfirmationMethods returns the confirmationMethods of this
     * SamlAssertionWrapper object.
     *
     * @return the confirmationMethods of this SamlAssertionWrapper object.
     */
    public List<String> getConfirmationMethods() {
        List<String> methods = new ArrayList<>();
        if (samlVersion == SAMLVersion.VERSION_20) {
            org.opensaml.saml.saml2.core.Subject subject =
                ((org.opensaml.saml.saml2.core.Assertion)samlObject).getSubject();
            List<org.opensaml.saml.saml2.core.SubjectConfirmation> confirmations =
                subject.getSubjectConfirmations();
            for (org.opensaml.saml.saml2.core.SubjectConfirmation confirmation : confirmations) {
                methods.add(confirmation.getMethod());
            }
        } else if (samlVersion == SAMLVersion.VERSION_11) {
            List<SubjectStatement> subjectStatements = new ArrayList<>();
            org.opensaml.saml.saml1.core.Assertion saml1 =
                (org.opensaml.saml.saml1.core.Assertion)samlObject;
            subjectStatements.addAll(saml1.getSubjectStatements());
            subjectStatements.addAll(saml1.getAuthenticationStatements());
            subjectStatements.addAll(saml1.getAttributeStatements());
            subjectStatements.addAll(saml1.getAuthorizationDecisionStatements());
            for (SubjectStatement subjectStatement : subjectStatements) {
                Subject subject = subjectStatement.getSubject();
                if (subject != null) {
                    SubjectConfirmation confirmation = subject.getSubjectConfirmation();
                    if (confirmation != null) {
                        XMLObject data = confirmation.getSubjectConfirmationData();
                        if (data instanceof ConfirmationMethod) {
                            ConfirmationMethod method = (ConfirmationMethod) data;
                            methods.add(method.getURI());
                        }
                        List<ConfirmationMethod> confirmationMethods =
                            confirmation.getConfirmationMethods();
                        for (ConfirmationMethod confirmationMethod : confirmationMethods) {
                            methods.add(confirmationMethod.getURI());
                        }
                    }
                }
            }
        }
        return methods;
    }

    /**
     * Method isSigned returns the signed of this SamlAssertionWrapper object.
     *
     * @return the signed (type boolean) of this SamlAssertionWrapper object.
     */
    public boolean isSigned() {
        return samlObject instanceof SignableSAMLObject
            && (((SignableSAMLObject)samlObject).isSigned()
                || ((SignableSAMLObject)samlObject).getSignature() != null);
    }

    /**
     * Method setSignature sets the signature of this SamlAssertionWrapper object.
     *
     * @param signature the signature of this SamlAssertionWrapper object.
     */
    public void setSignature(Signature signature) {
        setSignature(signature, defaultSignatureDigestAlgorithm);
    }

    /**
     * Method setSignature sets the signature of this SamlAssertionWrapper object.
     *
     * @param signature the signature of this SamlAssertionWrapper object.
     * @param signatureDigestAlgorithm the signature digest algorithm to use
     */
    public void setSignature(Signature signature, String signatureDigestAlgorithm) {
        if (samlObject instanceof SignableSAMLObject) {
            SignableSAMLObject signableObject = (SignableSAMLObject) samlObject;
            signableObject.setSignature(signature);
            String digestAlg = signatureDigestAlgorithm;
            if (digestAlg == null) {
                digestAlg = defaultSignatureDigestAlgorithm;
            }
            SAMLObjectContentReference contentRef =
                (SAMLObjectContentReference)signature.getContentReferences().get(0);
            contentRef.setDigestAlgorithm(digestAlg);
            signableObject.releaseDOM();
            signableObject.releaseChildrenDOM(true);
        } else {
            LOG.error("Attempt to sign an unsignable object " + samlObject.getClass().getName());
        }
    }

    /**
     * Create an enveloped signature on the assertion that has been created.
     *
     * @param issuerKeyName the Issuer KeyName to use with the issuerCrypto argument
     * @param issuerKeyPassword the Issuer Password to use with the issuerCrypto argument
     * @param issuerCrypto the Issuer Crypto instance
     * @param sendKeyValue whether to send the key value or not
     * @throws WSSecurityException
     */
    public void signAssertion(String issuerKeyName, String issuerKeyPassword,
            Crypto issuerCrypto, boolean sendKeyValue)
            throws WSSecurityException {

        signAssertion(issuerKeyName, issuerKeyPassword, issuerCrypto,
                sendKeyValue, defaultCanonicalizationAlgorithm,
                null, defaultSignatureDigestAlgorithm);
    }

    /**
     * Create an enveloped signature on the assertion that has been created.
     *
     * @param issuerKeyName the Issuer KeyName to use with the issuerCrypto argument
     * @param issuerKeyPassword the Issuer Password to use with the issuerCrypto argument
     * @param issuerCrypto the Issuer Crypto instance
     * @param sendKeyValue whether to send the key value or not
     * @param canonicalizationAlgorithm the canonicalization algorithm to be used for signing
     * @param signatureAlgorithm the signature algorithm to be used for signing
     * @throws WSSecurityException
     */
    public void signAssertion(String issuerKeyName, String issuerKeyPassword,
            Crypto issuerCrypto, boolean sendKeyValue,
            String canonicalizationAlgorithm, String signatureAlgorithm)
            throws WSSecurityException {
        signAssertion(issuerKeyName, issuerKeyPassword, issuerCrypto, sendKeyValue,
                canonicalizationAlgorithm, signatureAlgorithm, defaultSignatureDigestAlgorithm);
    }

    /**
     * Create an enveloped signature on the assertion that has been created.
     *
     * @param issuerKeyName the Issuer KeyName to use with the issuerCrypto argument
     * @param issuerKeyPassword the Issuer Password to use with the issuerCrypto argument
     * @param issuerCrypto the Issuer Crypto instance
     * @param sendKeyValue whether to send the key value or not
     * @param canonicalizationAlgorithm the canonicalization algorithm to be used for signing
     * @param signatureAlgorithm the signature algorithm to be used for signing
     * @param signatureDigestAlgorithm the signature Digest algorithm to use
     * @throws WSSecurityException
     */
    public void signAssertion(String issuerKeyName, String issuerKeyPassword,
            Crypto issuerCrypto, boolean sendKeyValue,
            String canonicalizationAlgorithm, String signatureAlgorithm,
            String signatureDigestAlgorithm)
            throws WSSecurityException {
        //
        // Create the signature
        //
        Signature signature = OpenSAMLUtil.buildSignature();
        String c14nAlgo = canonicalizationAlgorithm;
        if (c14nAlgo == null) {
            c14nAlgo = defaultCanonicalizationAlgorithm;
        }
        signature.setCanonicalizationAlgorithm(c14nAlgo);
        LOG.debug("Using Canonicalization algorithm {}", c14nAlgo);

        // prepare to sign the SAML token
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(issuerKeyName);
        X509Certificate[] issuerCerts = null;
        if (issuerCrypto != null) {
            issuerCerts = issuerCrypto.getX509Certificates(cryptoType);
        }
        if (issuerCerts == null || issuerCerts.length == 0) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                new Object[] {"No issuer certs were found to sign the SAML Assertion using issuer name: "
                                              + issuerKeyName});
        }

        String sigAlgo = signatureAlgorithm;
        if (sigAlgo == null) {
            sigAlgo = defaultRSASignatureAlgorithm;
            String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
            LOG.debug("automatic sig algo detection: {}", pubKeyAlgo);
            if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
                sigAlgo = defaultDSASignatureAlgorithm;
            } else if (pubKeyAlgo.equalsIgnoreCase("EC")) {
                sigAlgo = defaultECDSASignatureAlgorithm;
            }
        }
        LOG.debug("Using Signature algorithm {}", sigAlgo);
        PrivateKey privateKey;
        try {
            privateKey = issuerCrypto.getPrivateKey(issuerKeyName, issuerKeyPassword);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
        if (privateKey == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "empty",
                new Object[] {"No private key was found using issuer name: " + issuerKeyName});
        }

        signature.setSignatureAlgorithm(sigAlgo);

        BasicX509Credential signingCredential =
            new BasicX509Credential(issuerCerts[0], privateKey);

        signature.setSigningCredential(signingCredential);

        X509KeyInfoGeneratorFactory kiFactory = new X509KeyInfoGeneratorFactory();
        if (sendKeyValue) {
            kiFactory.setEmitPublicKeyValue(true);
        } else {
            kiFactory.setEmitEntityCertificate(true);
        }
        try {
            KeyInfo keyInfo = kiFactory.newInstance().generate(signingCredential);
            signature.setKeyInfo(keyInfo);
        } catch (org.opensaml.security.SecurityException ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                new Object[] {"Error generating KeyInfo from signing credential"});
        }

        // add the signature to the assertion
        setSignature(signature, signatureDigestAlgorithm);
    }

    /**
     * Verify the signature of this assertion
     *
     * @throws WSSecurityException
     */
    public void verifySignature(
        SAMLKeyInfoProcessor keyInfoProcessor, Crypto sigCrypto
    ) throws WSSecurityException {
        Signature sig = getSignature();
        if (sig != null) {
            KeyInfo keyInfo = sig.getKeyInfo();
            if (keyInfo == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"}
                );
            }
            SAMLKeyInfo samlKeyInfo =
                SAMLUtil.getCredentialFromKeyInfo(keyInfo.getDOM(), keyInfoProcessor, sigCrypto);
            verifySignature(samlKeyInfo);
        } else {
            LOG.debug("SamlAssertionWrapper: no signature to validate");
        }

    }

    /**
     * Verify the signature of this assertion
     *
     * @throws WSSecurityException
     */
    public void verifySignature(SAMLKeyInfo samlKeyInfo) throws WSSecurityException {
        Signature sig = getSignature();
        if (sig != null) {
            if (samlKeyInfo == null) {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"}
                );
            }

            BasicCredential credential = null;
            if (samlKeyInfo.getCerts() != null) {
                credential = new BasicX509Credential(samlKeyInfo.getCerts()[0]);
            } else if (samlKeyInfo.getPublicKey() != null) {
                credential = new BasicCredential(samlKeyInfo.getPublicKey());
            } else {
                throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity",
                    new Object[] {"cannot get certificate or key"}
                );
            }
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(SignerProvider.class.getClassLoader());
                SignatureValidator.validate(sig, credential);
            } catch (SignatureException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                        "empty", new Object[] {"SAML signature validation failed"});
            } finally {
                Thread.currentThread().setContextClassLoader(loader);
            }
            signatureKeyInfo = samlKeyInfo;
        } else {
            LOG.debug("SamlAssertionWrapper: no signature to validate");
        }
    }

    /**
     * Validate the signature of the Assertion against the Profile. This does not actually
     * verify the signature itself (see the verifySignature method for this)
     * @throws WSSecurityException
     */
    public void validateSignatureAgainstProfile() throws WSSecurityException {
        Signature sig = getSignature();
        if (sig != null) {
            SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
            try {
                validator.validate(sig);
            } catch (SignatureException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex,
                    "empty", new Object[] {"SAML signature validation failed"});
            }
        }
    }

    /**
     * This method parses the KeyInfo of the Subject. It then stores the SAMLKeyInfo object that
     * has been obtained for future processing.
     * @throws WSSecurityException
     */
    public void parseSubject(
        SAMLKeyInfoProcessor keyInfoProcessor,
        Crypto sigCrypto
    ) throws WSSecurityException {
        if (samlVersion == SAMLVersion.VERSION_11) {
            subjectKeyInfo =
                SAMLUtil.getCredentialFromSubject(
                    (org.opensaml.saml.saml1.core.Assertion)samlObject, keyInfoProcessor, sigCrypto
                );
        } else if (samlVersion == SAMLVersion.VERSION_20) {
            subjectKeyInfo =
                SAMLUtil.getCredentialFromSubject(
                    (org.opensaml.saml.saml2.core.Assertion)samlObject, keyInfoProcessor, sigCrypto
                );
        }
    }


    /**
     * Method getSamlVersion returns the samlVersion of this SamlAssertionWrapper object.
     *
     * @return the samlVersion (type SAMLVersion) of this SamlAssertionWrapper object.
     */
    public SAMLVersion getSamlVersion() {
        if (samlVersion == null) {
            // Try to set the version.
            LOG.debug(
                "The SAML version was null in getSamlVersion(). Recomputing SAML version...");
            if (samlObject instanceof org.opensaml.saml.saml1.core.Assertion) {
                samlVersion = SAMLVersion.VERSION_11;
            } else if (samlObject instanceof org.opensaml.saml.saml2.core.Assertion) {
                samlVersion = SAMLVersion.VERSION_20;
            } else {
                // We are only supporting SAML v1.1 or SAML v2.0 at this time.
                throw new IllegalStateException(
                    "Could not determine the SAML version number. Check your "
                    + "configuration and try again."
                );
            }
        }
        return samlVersion;
    }

    /**
     * Get the Assertion as a DOM Element.
     * @return the assertion as a DOM Element
     */
    public Element getElement() {
        return assertionElement;
    }

    /**
     * Get the SAMLKeyInfo associated with the signature of the assertion
     * @return the SAMLKeyInfo associated with the signature of the assertion
     */
    public SAMLKeyInfo getSignatureKeyInfo() {
        return signatureKeyInfo;
    }

    /**
     * Get the SAMLKeyInfo associated with the Subject KeyInfo
     * @return the SAMLKeyInfo associated with the Subject KeyInfo
     */
    public SAMLKeyInfo getSubjectKeyInfo() {
        return subjectKeyInfo;
    }

    /**
     * Get the SignatureValue bytes of the signed SAML Assertion
     * @return the SignatureValue bytes of the signed SAML Assertion
     * @throws WSSecurityException
     */
    public byte[] getSignatureValue() throws WSSecurityException {
        Signature sig = null;
        if (samlObject instanceof SignableSAMLObject) {
            sig = ((SignableSAMLObject)samlObject).getSignature();
        }
        if (sig != null) {
            return getSignatureValue(sig);
        }
        return new byte[0];
    }

    private byte[] getSignatureValue(Signature signature) throws WSSecurityException {
        Element signatureElement = signature.getDOM();

        if (signatureElement != null) {
            Element signedInfoElem = XMLUtils.getNextElement(signatureElement.getFirstChild());
            if (signedInfoElem != null) {
                Element signatureValueElement =
                    XMLUtils.getNextElement(signedInfoElem.getNextSibling());
                if (signatureValueElement != null) {
                    String base64Input = XMLUtils.getFullTextChildrenFromNode(signatureValueElement);
                    return XMLUtils.decode(base64Input);
                }
            }
        }

        return new byte[0];
    }

    public Signature getSignature() throws WSSecurityException {
        if (samlObject instanceof SignableSAMLObject) {
            return ((SignableSAMLObject)samlObject).getSignature();
        }
        return null;
    }

    public SAMLObject getSamlObject() {
        return samlObject;
    }

    /**
     * Check the Conditions of the Assertion.
     */
    public void checkConditions(int futureTTL) throws WSSecurityException {
        Instant validFrom = null;
        Instant validTill = null;

        if (getSamlVersion().equals(SAMLVersion.VERSION_20)
            && getSaml2().getConditions() != null) {
            validFrom = getSaml2().getConditions().getNotBefore();
            validTill = getSaml2().getConditions().getNotOnOrAfter();
        } else if (getSamlVersion().equals(SAMLVersion.VERSION_11)
            && getSaml1().getConditions() != null) {
            validFrom = getSaml1().getConditions().getNotBefore();
            validTill = getSaml1().getConditions().getNotOnOrAfter();
        }

        if (validFrom != null) {
            Instant currentTime = Instant.now();
            currentTime = currentTime.plusSeconds(futureTTL);
            if (validFrom.isAfter(currentTime)) {
                LOG.warn("SAML Token condition (Not Before) not met");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }
        }

        if (validTill != null && validTill.isBefore(Instant.now())) {
            LOG.warn("SAML Token condition (Not On Or After) not met");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
    }

    /**
     * Check the IssueInstant value of the Assertion.
     */
    public void checkIssueInstant(int futureTTL, int ttl) throws WSSecurityException {
        Instant issueInstant = null;
        Instant validTill = null;

        if (getSamlVersion().equals(SAMLVersion.VERSION_20)
            && getSaml2().getConditions() != null) {
            validTill = getSaml2().getConditions().getNotOnOrAfter();
            issueInstant = getSaml2().getIssueInstant();
        } else if (getSamlVersion().equals(SAMLVersion.VERSION_11)
            && getSaml1().getConditions() != null) {
            validTill = getSaml1().getConditions().getNotOnOrAfter();
            issueInstant = getSaml1().getIssueInstant();
        }

        // Check the IssueInstant is not in the future, subject to the future TTL
        if (issueInstant != null) {
            Instant currentTime = Instant.now().plusSeconds(futureTTL);
            if (issueInstant.isAfter(currentTime)) {
                LOG.warn("SAML Token IssueInstant not met");
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
            }

            // If there is no NotOnOrAfter, then impose a TTL on the IssueInstant.
            if (validTill == null) {
                currentTime = currentTime.minusSeconds(ttl);

                if (issueInstant.isBefore(currentTime)) {
                    LOG.warn("SAML Token IssueInstant not met. The assertion was created too long ago.");
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                }
            }
        }

    }

    /**
     * Check the AudienceRestrictions of the Assertion
     */
    public void checkAudienceRestrictions(List<String> audienceRestrictions) throws WSSecurityException {
        // Now check the audience restriction conditions
        if (audienceRestrictions == null || audienceRestrictions.isEmpty()) {
            return;
        }

        if (getSamlVersion().equals(SAMLVersion.VERSION_20) && getSaml2().getConditions() != null) {
            org.opensaml.saml.saml2.core.Conditions conditions = getSaml2().getConditions();
            if (conditions != null && conditions.getAudienceRestrictions() != null
                && !conditions.getAudienceRestrictions().isEmpty()) {
                boolean foundAddress = false;
                for (org.opensaml.saml.saml2.core.AudienceRestriction audienceRestriction
                    : conditions.getAudienceRestrictions()) {
                    if (audienceRestriction.getAudiences() != null) {
                        List<org.opensaml.saml.saml2.core.Audience> audiences =
                            audienceRestriction.getAudiences();
                        for (org.opensaml.saml.saml2.core.Audience audience : audiences) {
                            String audienceURI = audience.getURI();
                            if (audienceRestrictions.contains(audienceURI)) {
                                foundAddress = true;
                                break;
                            }
                        }
                    }
                }

                if (!foundAddress) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                }
            }
        } else if (getSamlVersion().equals(SAMLVersion.VERSION_11) && getSaml1().getConditions() != null) {
            org.opensaml.saml.saml1.core.Conditions conditions = getSaml1().getConditions();
            if (conditions != null && conditions.getAudienceRestrictionConditions() != null
                && !conditions.getAudienceRestrictionConditions().isEmpty()) {
                boolean foundAddress = false;
                for (org.opensaml.saml.saml1.core.AudienceRestrictionCondition audienceRestriction
                    : conditions.getAudienceRestrictionConditions()) {
                    if (audienceRestriction.getAudiences() != null) {
                        List<org.opensaml.saml.saml1.core.Audience> audiences =
                            audienceRestriction.getAudiences();
                        for (org.opensaml.saml.saml1.core.Audience audience : audiences) {
                            String audienceURI = audience.getURI();
                            if (audienceRestrictions.contains(audienceURI)) {
                                foundAddress = true;
                                break;
                            }
                        }
                    }
                }

                if (!foundAddress) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
                }
            }
        }
    }

    /**
     * Check the various attributes of the AuthnStatements of the assertion (if any)
     */
    public void checkAuthnStatements(int futureTTL) throws WSSecurityException {
        if (getSamlVersion().equals(SAMLVersion.VERSION_20)
            && getSaml2().getAuthnStatements() != null) {
            List<AuthnStatement> authnStatements = getSaml2().getAuthnStatements();

            for (AuthnStatement authnStatement : authnStatements) {
                Instant authnInstant = authnStatement.getAuthnInstant();
                Instant sessionNotOnOrAfter = authnStatement.getSessionNotOnOrAfter();
                String subjectLocalityAddress = null;

                if (authnStatement.getSubjectLocality() != null
                    && authnStatement.getSubjectLocality().getAddress() != null) {
                    subjectLocalityAddress = authnStatement.getSubjectLocality().getAddress();
                }

                validateAuthnStatement(authnInstant, sessionNotOnOrAfter,
                                       subjectLocalityAddress, futureTTL);
            }
        } else if (getSamlVersion().equals(SAMLVersion.VERSION_11)
            && getSaml1().getAuthenticationStatements() != null) {
            List<AuthenticationStatement> authnStatements =
                getSaml1().getAuthenticationStatements();

            for (AuthenticationStatement authnStatement : authnStatements) {
                Instant authnInstant = authnStatement.getAuthenticationInstant();
                String subjectLocalityAddress = null;

                if (authnStatement.getSubjectLocality() != null
                    && authnStatement.getSubjectLocality().getIPAddress() != null) {
                    subjectLocalityAddress = authnStatement.getSubjectLocality().getIPAddress();
                }

                validateAuthnStatement(authnInstant, null,
                                       subjectLocalityAddress, futureTTL);
            }
        }
    }

    private void validateAuthnStatement(
        Instant authnInstant, Instant sessionNotOnOrAfter, String subjectLocalityAddress,
        int futureTTL
    ) throws WSSecurityException {
        // AuthnInstant in the future
        Instant currentTime = Instant.now();
        currentTime = currentTime.plusSeconds(futureTTL);
        if (authnInstant.isAfter(currentTime)) {
            LOG.warn("SAML Token AuthnInstant not met");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        // Stale SessionNotOnOrAfter
        if (sessionNotOnOrAfter != null && sessionNotOnOrAfter.isBefore(Instant.now())) {
            LOG.warn("SAML Token SessionNotOnOrAfter not met");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }

        // Check that the SubjectLocality address is an IP address
        if (subjectLocalityAddress != null
            && !(InetAddressUtils.isIPv4Address(subjectLocalityAddress)
                || InetAddressUtils.isIPv6Address(subjectLocalityAddress))) {
            LOG.warn("SAML Token SubjectLocality address is not valid: " + subjectLocalityAddress);
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidSAMLsecurity");
        }
    }

    /**
     * Parse the DOM Element into Opensaml objects.
     */
    private void parseElement(Element element) throws WSSecurityException {
        XMLObject xmlObject = OpenSAMLUtil.fromDom(element);
        if (xmlObject instanceof org.opensaml.saml.saml1.core.Assertion) {
            this.samlObject = (SAMLObject)xmlObject;
            samlVersion = SAMLVersion.VERSION_11;
        } else if (xmlObject instanceof org.opensaml.saml.saml2.core.Assertion) {
            this.samlObject = (SAMLObject)xmlObject;
            samlVersion = SAMLVersion.VERSION_20;
        } else {
            LOG.error(
                "SamlAssertionWrapper: found unexpected type " + xmlObject.getClass().getName()
            );
        }

        assertionElement = element;
    }

    /**
     * Parse a SAMLCallback object to create a SAML Assertion
     */
    private void parseCallback(
        SAMLCallback samlCallback
    ) throws WSSecurityException {
        samlVersion = samlCallback.getSamlVersion();
        if (samlVersion == null) {
            samlVersion = SAMLVersion.VERSION_20;
        }
        String issuer = samlCallback.getIssuer();
        String issuerFormat = samlCallback.getIssuerFormat();
        String issuerQualifier = samlCallback.getIssuerQualifier();

        if (samlVersion.equals(SAMLVersion.VERSION_11)) {
            // Build a SAML v1.1 assertion
            org.opensaml.saml.saml1.core.Assertion saml1 =
                SAML1ComponentBuilder.createSamlv1Assertion(issuer);

            try {
                // Process the SAML authentication statement(s)
                List<AuthenticationStatement> authenticationStatements =
                    SAML1ComponentBuilder.createSamlv1AuthenticationStatement(
                        samlCallback.getAuthenticationStatementData()
                    );
                saml1.getAuthenticationStatements().addAll(authenticationStatements);

                // Process the SAML attribute statement(s)
                List<AttributeStatement> attributeStatements =
                        SAML1ComponentBuilder.createSamlv1AttributeStatement(
                            samlCallback.getAttributeStatementData()
                        );
                saml1.getAttributeStatements().addAll(attributeStatements);

                // Process the SAML authorization decision statement(s)
                List<AuthorizationDecisionStatement> authDecisionStatements =
                        SAML1ComponentBuilder.createSamlv1AuthorizationDecisionStatement(
                            samlCallback.getAuthDecisionStatementData()
                        );
                saml1.getAuthorizationDecisionStatements().addAll(authDecisionStatements);

                // Build the complete assertion
                org.opensaml.saml.saml1.core.Conditions conditions =
                    SAML1ComponentBuilder.createSamlv1Conditions(samlCallback.getConditions());
                saml1.setConditions(conditions);

                if (samlCallback.getAdvice() != null) {
                    org.opensaml.saml.saml1.core.Advice advice =
                        SAML1ComponentBuilder.createAdvice(samlCallback.getAdvice());
                    saml1.setAdvice(advice);
                }
            } catch (org.opensaml.security.SecurityException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                    new Object[] {"Error generating KeyInfo from signing credential"}
                );
            }

            // Set the OpenSaml2 XMLObject instance
            samlObject = saml1;

        } else if (samlVersion.equals(SAMLVersion.VERSION_20)) {
            // Build a SAML v2.0 assertion
            org.opensaml.saml.saml2.core.Assertion saml2 = SAML2ComponentBuilder.createAssertion();
            Issuer samlIssuer = SAML2ComponentBuilder.createIssuer(issuer, issuerFormat, issuerQualifier);

            // Authn Statement(s)
            List<AuthnStatement> authnStatements =
                SAML2ComponentBuilder.createAuthnStatement(
                    samlCallback.getAuthenticationStatementData()
                );
            saml2.getAuthnStatements().addAll(authnStatements);

            // Attribute statement(s)
            List<org.opensaml.saml.saml2.core.AttributeStatement> attributeStatements =
                SAML2ComponentBuilder.createAttributeStatement(
                    samlCallback.getAttributeStatementData()
                );
            saml2.getAttributeStatements().addAll(attributeStatements);

            // AuthzDecisionStatement(s)
            List<AuthzDecisionStatement> authDecisionStatements =
                    SAML2ComponentBuilder.createAuthorizationDecisionStatement(
                        samlCallback.getAuthDecisionStatementData()
                    );
            saml2.getAuthzDecisionStatements().addAll(authDecisionStatements);

            // Build the SAML v2.0 assertion
            saml2.setIssuer(samlIssuer);

            try {
                org.opensaml.saml.saml2.core.Subject subject =
                    SAML2ComponentBuilder.createSaml2Subject(samlCallback.getSubject());
                saml2.setSubject(subject);
            } catch (org.opensaml.security.SecurityException ex) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "empty",
                    new Object[] {"Error generating KeyInfo from signing credential"}
                );
            }

            org.opensaml.saml.saml2.core.Conditions conditions =
                SAML2ComponentBuilder.createConditions(samlCallback.getConditions());
            saml2.setConditions(conditions);

            if (samlCallback.getAdvice() != null) {
                org.opensaml.saml.saml2.core.Advice advice =
                    SAML2ComponentBuilder.createAdvice(samlCallback.getAdvice());
                saml2.setAdvice(advice);
            }

            // Set the OpenSaml2 XMLObject instance
            samlObject = saml2;
        }
    }

}

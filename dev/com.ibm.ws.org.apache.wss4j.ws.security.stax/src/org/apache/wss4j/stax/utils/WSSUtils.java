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
package org.apache.wss4j.stax.utils;

import java.io.IOException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;

import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.securityEvent.DerivedKeyTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.EncryptedKeyTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.HttpsTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.KerberosTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.KeyValueTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.RelTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.SamlTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.SecurityContextTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.UsernameTokenSecurityEvent;
import org.apache.wss4j.stax.securityEvent.X509TokenSecurityEvent;
import org.apache.wss4j.stax.securityToken.WSSecurityTokenConstants;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.AbstractOutputProcessor;
import org.apache.xml.security.stax.ext.OutputProcessorChain;
import org.apache.xml.security.stax.ext.SecurePart;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityUtils;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.EncryptionPartDef;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityToken;
import org.apache.xml.security.utils.XMLUtils;

public class WSSUtils extends XMLSecurityUtils {

    protected WSSUtils() {
        super();
    }

    /**
     * Executes the Callback handling. Typically used to fetch passwords
     *
     * @param callbackHandler
     * @param callback
     * @throws WSSecurityException if the callback couldn't be executed
     */
    public static void doPasswordCallback(CallbackHandler callbackHandler, Callback callback)
            throws WSSecurityException {

        if (callbackHandler == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCallback");
        }
        try {
            callbackHandler.handle(new Callback[]{callback});
        } catch (IOException | UnsupportedCallbackException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
    }

    /**
     * Try to get the secret key from a CallbackHandler implementation
     *
     * @param callbackHandler a CallbackHandler implementation
     * @throws WSSecurityException
     */
    public static void doSecretKeyCallback(CallbackHandler callbackHandler, Callback callback, String id)
            throws WSSecurityException {

        if (callbackHandler != null) {
            try {
                callbackHandler.handle(new Callback[]{callback});
            } catch (IOException | UnsupportedCallbackException e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e, "noPassword");
            }
        }
    }

    public static String getSOAPMessageVersionNamespace(XMLSecEvent xmlSecEvent) {
        XMLSecStartElement xmlSecStartElement = xmlSecEvent.getStartElementAtLevel(1);
        if (xmlSecStartElement != null) {
            if (WSSConstants.TAG_SOAP11_ENVELOPE.equals(xmlSecStartElement.getName())) {
                return WSSConstants.NS_SOAP11;
            } else if (WSSConstants.TAG_SOAP12_ENVELOPE.equals(xmlSecStartElement.getName())) {
                return WSSConstants.NS_SOAP12;
            }
        }
        return null;
    }

    public static boolean isInSOAPHeader(XMLSecEvent xmlSecEvent) {
        final List<QName> elementPath = xmlSecEvent.getElementPath();
        return isInSOAPHeader(elementPath);
    }

    public static boolean isInSOAPHeader(List<QName> elementPath) {
        if (elementPath.size() > 1) {
            final QName secondLevelElementName = elementPath.get(1);
            return WSSConstants.TAG_SOAP_HEADER_LN.equals(secondLevelElementName.getLocalPart())
                    && elementPath.get(0).getNamespaceURI().equals(secondLevelElementName.getNamespaceURI());
        }
        return false;
    }

    public static boolean isInSOAPBody(XMLSecEvent xmlSecEvent) {
        final List<QName> elementPath = xmlSecEvent.getElementPath();
        return isInSOAPBody(elementPath);
    }

    public static boolean isInSOAPBody(List<QName> elementPath) {
        if (elementPath.size() > 1) {
            final QName secondLevelElementName = elementPath.get(1);
            return WSSConstants.TAG_SOAP_BODY_LN.equals(secondLevelElementName.getLocalPart())
                    && elementPath.get(0).getNamespaceURI().equals(secondLevelElementName.getNamespaceURI());
        }
        return false;
    }

    public static boolean isInSecurityHeader(XMLSecEvent xmlSecEvent, String actorOrRole) {
        final List<QName> elementPath = xmlSecEvent.getElementPath();
        return isInSecurityHeader(xmlSecEvent, elementPath, actorOrRole);
    }

    public static boolean isInSecurityHeader(XMLSecEvent xmlSecEvent, List<QName> elementPath, String actorOrRole) {
        if (elementPath.size() > 2) {
            final QName secondLevelElementName = elementPath.get(1);
            return WSSConstants.TAG_WSSE_SECURITY.equals(elementPath.get(2))
                    && isResponsibleActorOrRole(xmlSecEvent.getStartElementAtLevel(3), actorOrRole)
                    && WSSConstants.TAG_SOAP_HEADER_LN.equals(secondLevelElementName.getLocalPart())
                    && elementPath.get(0).getNamespaceURI().equals(secondLevelElementName.getNamespaceURI());
        }
        return false;
    }

    public static boolean isSecurityHeaderElement(XMLSecEvent xmlSecEvent, String actorOrRole) {
        if (!xmlSecEvent.isStartElement()) {
            return false;
        }

        final List<QName> elementPath = xmlSecEvent.getElementPath();
        if (elementPath.size() == 3) {
            final QName secondLevelElementName = elementPath.get(1);
            return WSSConstants.TAG_WSSE_SECURITY.equals(elementPath.get(2))
                    && isResponsibleActorOrRole(xmlSecEvent.getStartElementAtLevel(3), actorOrRole)
                    && WSSConstants.TAG_SOAP_HEADER_LN.equals(secondLevelElementName.getLocalPart())
                    && elementPath.get(0).getNamespaceURI().equals(secondLevelElementName.getNamespaceURI());
        }
        return false;
    }

    public static boolean isResponsibleActorOrRole(XMLSecStartElement xmlSecStartElement, String responsibleActor) {
        final QName actorRole;
        final String soapVersionNamespace = getSOAPMessageVersionNamespace(xmlSecStartElement);
        if (WSSConstants.NS_SOAP11.equals(soapVersionNamespace)) {
            actorRole = WSSConstants.ATT_SOAP11_ACTOR;
        } else {
            actorRole = WSSConstants.ATT_SOAP12_ROLE;
        }

        String actor = null;
        Attribute attribute = xmlSecStartElement.getAttributeByName(actorRole);
        if (attribute != null) {
            actor = attribute.getValue();
        }

        if (responsibleActor == null) {
            return actor == null;
        } else {
            return responsibleActor.equals(actor);
        }
    }

    public static void createBinarySecurityTokenStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                          OutputProcessorChain outputProcessorChain,
                                                          String referenceId, X509Certificate[] x509Certificates,
                                                          boolean useSingleCertificate)
            throws XMLStreamException, XMLSecurityException {
        String valueType;
        if (useSingleCertificate) {
            valueType = WSSConstants.NS_X509_V3_TYPE;
        } else {
            valueType = WSSConstants.NS_X509_PKIPATH_V1;
        }
        List<XMLSecAttribute> attributes = new ArrayList<>(3);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, valueType));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_WSU_ID, referenceId));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                                                                   WSSConstants.TAG_WSSE_BINARY_SECURITY_TOKEN,
                                                                   false, attributes);
        try {
            if (useSingleCertificate) {
                String encodedCert =
                    XMLUtils.encodeToString(x509Certificates[0].getEncoded());
                abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, encodedCert);
            } else {
                try {
                    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    List<X509Certificate> certificates = Arrays.asList(x509Certificates);
                    String encodedCert =
                        XMLUtils.encodeToString(certificateFactory.generateCertPath(certificates).getEncoded());
                    abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, encodedCert);
                } catch (CertificateException e) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
                }
            }
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                                                                 WSSConstants.TAG_WSSE_BINARY_SECURITY_TOKEN);
    }

    public static void createX509SubjectKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                               OutputProcessorChain outputProcessorChain,
                                                               X509Certificate[] x509Certificates)
            throws XMLSecurityException, XMLStreamException {
        // As per the 1.1 specification, SKI can only be used for a V3 certificate
        if (x509Certificates[0].getVersion() != 3) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "invalidCertForSKI");
        }

        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_X509_SKI));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                                                                   WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        byte[] data = new Merlin().getSKIBytesFromCert(x509Certificates[0]);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain,
                                                                 XMLUtils.encodeToString(data));
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                                                                 WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createX509KeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                        OutputProcessorChain outputProcessorChain,
                                                        X509Certificate[] x509Certificates)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_X509_V3_TYPE));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                                                                   WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        try {
            String encodedCert = XMLUtils.encodeToString(x509Certificates[0].getEncoded());
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, encodedCert);
        } catch (CertificateEncodingException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                                                                 WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createThumbprintKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                              OutputProcessorChain outputProcessorChain,
                                                              X509Certificate[] x509Certificates)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_THUMBPRINT));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                                                                   WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] data = sha.digest(x509Certificates[0].getEncoded());
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain,
                                                                     XMLUtils.encodeToString(data));
        } catch (CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createEncryptedKeySha1IdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                                 OutputProcessorChain outputProcessorChain, Key key)
            throws XMLStreamException, XMLSecurityException {

        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            byte[] data = sha.digest(key.getEncoded());
            createEncryptedKeySha1IdentifierStructure(abstractOutputProcessor, outputProcessorChain,
                                                      XMLUtils.encodeToString(data));
        } catch (NoSuchAlgorithmException e) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
        }
    }

    public static void createEncryptedKeySha1IdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                                 OutputProcessorChain outputProcessorChain, String identifier)
            throws XMLStreamException, XMLSecurityException {

        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_ENCRYPTED_KEY_SHA1));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, identifier);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createKerberosSha1IdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                                 OutputProcessorChain outputProcessorChain, String identifier)
            throws XMLStreamException, XMLSecurityException {

        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_ENCODING_TYPE,
                                                               WSSConstants.SOAPMESSAGE_NS10_BASE64_ENCODING));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_KERBEROS5_AP_REQ_SHA1));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, identifier);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createBSTReferenceStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                   OutputProcessorChain outputProcessorChain, String referenceId,
                                                   String valueType, boolean includedInMessage)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        String uri = includedInMessage ? "#" + referenceId : referenceId;
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_URI, uri));
        if (valueType != null) {
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, valueType));
        }
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_REFERENCE,
                                                                   false, attributes);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_REFERENCE);
    }

    public static void createEmbeddedKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                            OutputProcessorChain outputProcessorChain,
                                                            WSSecurityTokenConstants.TokenType tokenType, String referenceId)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(1);
        if (WSSecurityTokenConstants.SAML_10_TOKEN.equals(tokenType) || WSSecurityTokenConstants.SAML_11_TOKEN.equals(tokenType)) {
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, WSSConstants.NS_SAML10_TYPE));
        } else if (WSSecurityTokenConstants.SAML_20_TOKEN.equals(tokenType)) {
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, WSSConstants.NS_SAML20_TYPE));
        }
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, referenceId);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createSAMLKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                            OutputProcessorChain outputProcessorChain,
                                                            WSSecurityTokenConstants.TokenType tokenType, String referenceId)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(1);
        if (WSSecurityTokenConstants.SAML_10_TOKEN.equals(tokenType) || WSSecurityTokenConstants.SAML_11_TOKEN.equals(tokenType)) {
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, WSSConstants.NS_SAML10_TYPE));
        } else if (WSSecurityTokenConstants.SAML_20_TOKEN.equals(tokenType)) {
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE, WSSConstants.NS_SAML20_TYPE));
        }
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER,
                                                                   false, attributes);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, referenceId);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_KEY_IDENTIFIER);
    }

    public static void createUsernameTokenReferenceStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                             OutputProcessorChain outputProcessorChain, String tokenId)
            throws XMLStreamException, XMLSecurityException {
        List<XMLSecAttribute> attributes = new ArrayList<>(2);
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_URI, "#" + tokenId));
        attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_VALUE_TYPE,
                                                               WSSConstants.NS_USERNAMETOKEN_PROFILE_USERNAME_TOKEN));
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_REFERENCE,
                                                                   false, attributes);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, WSSConstants.TAG_WSSE_REFERENCE);
    }

    public static void createReferenceListStructureForEncryption(AbstractOutputProcessor abstractOutputProcessor,
                                                             OutputProcessorChain outputProcessorChain)
            throws XMLStreamException, XMLSecurityException {
        List<EncryptionPartDef> encryptionPartDefs =
                outputProcessorChain.getSecurityContext().getAsList(EncryptionPartDef.class);
        if (encryptionPartDefs == null) {
            return;
        }
        List<XMLSecAttribute> attributes;
        abstractOutputProcessor.createStartElementAndOutputAsEvent(
                outputProcessorChain, XMLSecurityConstants.TAG_xenc_ReferenceList, true, null);
        //output the references to the encrypted data:
        Iterator<EncryptionPartDef> encryptionPartDefIterator = encryptionPartDefs.iterator();
        while (encryptionPartDefIterator.hasNext()) {
            EncryptionPartDef encryptionPartDef = encryptionPartDefIterator.next();

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(
                    XMLSecurityConstants.ATT_NULL_URI, "#" + encryptionPartDef.getEncRefId()));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_xenc_DataReference, false, attributes);
            final String compressionAlgorithm =
                    ((WSSSecurityProperties)abstractOutputProcessor.getSecurityProperties()).getEncryptionCompressionAlgorithm();
            if (compressionAlgorithm != null) {
                abstractOutputProcessor.createStartElementAndOutputAsEvent(
                        outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transforms, true, null);
                attributes = new ArrayList<>(1);
                attributes.add(abstractOutputProcessor.createAttribute(
                        XMLSecurityConstants.ATT_NULL_Algorithm, compressionAlgorithm));
                abstractOutputProcessor.createStartElementAndOutputAsEvent(
                        outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform, false, attributes);
                abstractOutputProcessor.createEndElementAndOutputAsEvent(
                        outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform);
                abstractOutputProcessor.createEndElementAndOutputAsEvent(
                        outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transforms);
            }
            abstractOutputProcessor.createEndElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_xenc_DataReference);
        }
        abstractOutputProcessor.createEndElementAndOutputAsEvent(
                outputProcessorChain, XMLSecurityConstants.TAG_xenc_ReferenceList);
    }

    public static void createEncryptedDataStructureForAttachments(
            AbstractOutputProcessor abstractOutputProcessor, OutputProcessorChain outputProcessorChain)
            throws XMLStreamException, XMLSecurityException {

        List<EncryptionPartDef> encryptionPartDefs =
                outputProcessorChain.getSecurityContext().getAsList(EncryptionPartDef.class);
        if (encryptionPartDefs == null) {
            return;
        }

        Iterator<EncryptionPartDef> encryptionPartDefIterator = encryptionPartDefs.iterator();
        while (encryptionPartDefIterator.hasNext()) {
            EncryptionPartDef encryptionPartDef = encryptionPartDefIterator.next();

            if (encryptionPartDef.getCipherReferenceId() == null) {
                continue;
            }

            List<XMLSecAttribute> attributes = new ArrayList<>(3);
            attributes.add(abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_Id,
                    encryptionPartDef.getEncRefId()));
            if (encryptionPartDef.getModifier() == SecurePart.Modifier.Element) {
                attributes.add(
                        abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_Type,
                                WSSConstants.SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_COMPLETE));
            } else {
                attributes.add(
                        abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_Type,
                                WSSConstants.SWA_ATTACHMENT_ENCRYPTED_DATA_TYPE_CONTENT_ONLY));
            }
            attributes.add(
                    abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_MimeType,
                            encryptionPartDef.getMimeType()));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_EncryptedData, true, attributes);

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_Algorithm,
                    abstractOutputProcessor.getSecurityProperties().getEncryptionSymAlgorithm()));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_EncryptionMethod, false, attributes);

            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_EncryptionMethod);

            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_dsig_KeyInfo, true, null);

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_WSSE11_TOKEN_TYPE,
                    WSSConstants.NS_WSS_ENC_KEY_VALUE_TYPE));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE, true, attributes);

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_URI,
                    "#" + encryptionPartDef.getKeyId()));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    WSSConstants.TAG_WSSE_REFERENCE, false, attributes);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    WSSConstants.TAG_WSSE_REFERENCE);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    WSSConstants.TAG_WSSE_SECURITY_TOKEN_REFERENCE);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_dsig_KeyInfo);

            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_CipherData, false, null);

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(WSSConstants.ATT_NULL_URI,
                    "cid:" + encryptionPartDef.getCipherReferenceId()));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_CipherReference, false, attributes);

            abstractOutputProcessor.createStartElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_xenc_Transforms, false, null);

            attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(
                    XMLSecurityConstants.ATT_NULL_Algorithm, WSSConstants.SWA_ATTACHMENT_CIPHERTEXT_TRANS));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform, true, attributes);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transform);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(
                    outputProcessorChain, XMLSecurityConstants.TAG_dsig_Transforms);

            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_CipherReference);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_CipherData);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain,
                    XMLSecurityConstants.TAG_xenc_EncryptedData);
        }
    }

    @SuppressWarnings("unchecked")
    public static TokenSecurityEvent<? extends InboundSecurityToken>
        createTokenSecurityEvent(final InboundSecurityToken inboundSecurityToken, String correlationID)
            throws WSSecurityException {
        WSSecurityTokenConstants.TokenType tokenType = inboundSecurityToken.getTokenType();

        TokenSecurityEvent<? extends SecurityToken> tokenSecurityEvent;
        if (WSSecurityTokenConstants.X509V1Token.equals(tokenType)
            || WSSecurityTokenConstants.X509V3Token.equals(tokenType)
            || WSSecurityTokenConstants.X509Pkcs7Token.equals(tokenType)
            || WSSecurityTokenConstants.X509PkiPathV1Token.equals(tokenType)) {
            tokenSecurityEvent = new X509TokenSecurityEvent();
        } else if (WSSecurityTokenConstants.USERNAME_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new UsernameTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.KERBEROS_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new KerberosTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.SECURITY_CONTEXT_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new SecurityContextTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.SAML_10_TOKEN.equals(tokenType)
            || WSSecurityTokenConstants.SAML_11_TOKEN.equals(tokenType)
            || WSSecurityTokenConstants.SAML_20_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new SamlTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.REL_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new RelTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.HTTPS_TOKEN.equals(tokenType)) {
            tokenSecurityEvent = new HttpsTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.KeyValueToken.equals(tokenType)) {
            tokenSecurityEvent = new KeyValueTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.DerivedKeyToken.equals(tokenType)) {
            tokenSecurityEvent = new DerivedKeyTokenSecurityEvent();
        } else if (WSSecurityTokenConstants.EncryptedKeyToken.equals(tokenType)) {
            tokenSecurityEvent = new EncryptedKeyTokenSecurityEvent();
        } else {
            throw new WSSecurityException(WSSecurityException.ErrorCode.UNSUPPORTED_SECURITY_TOKEN);
        }
        ((TokenSecurityEvent<SecurityToken>)tokenSecurityEvent).setSecurityToken(inboundSecurityToken);
        tokenSecurityEvent.setCorrelationID(correlationID);
        return (TokenSecurityEvent<? extends InboundSecurityToken>)tokenSecurityEvent;
    }

    public static boolean pathMatches(List<QName> path1, List<QName> path2) {
        return pathMatches(path1, path2, false);
    }

    public static boolean pathMatches(List<QName> path1, List<QName> path2, boolean lastElementWildCard) {
        if (path1 == null) {
            throw new IllegalArgumentException("Internal error");
        }
        if (path2 == null || path1.size() != path2.size()) {
            return false;
        }
        Iterator<QName> path1Iterator = path1.iterator();
        Iterator<QName> path2Iterator = path2.iterator();
        while (path1Iterator.hasNext()) {
            QName qName1 = path1Iterator.next();
            QName qName2 = path2Iterator.next();
            if (!qName1.equals(qName2)) {
                if (!path1Iterator.hasNext() && lastElementWildCard) {
                    if (!qName1.getNamespaceURI().equals(qName2.getNamespaceURI())) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    public static String pathAsString(List<QName> path) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<QName> pathIterator = path.iterator();
        while (pathIterator.hasNext()) {
            QName qName = pathIterator.next();
            stringBuilder.append('/');
            stringBuilder.append(qName.toString());
        }
        return stringBuilder.toString();
    }

    @SuppressWarnings("unchecked")
    public static <T extends SecurityToken> T getRootToken(T securityToken) throws XMLSecurityException {
        T tmp = securityToken;
        while (tmp.getKeyWrappingToken() != null) {
            tmp = (T)tmp.getKeyWrappingToken();
        }
        return tmp;
    }

}

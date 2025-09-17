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
package org.apache.xml.security.stax.ext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.xml.security.algorithms.JCEMapper;
import org.apache.xml.security.algorithms.implementations.ECDSAUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.x509.XMLX509SKI;
import org.apache.xml.security.stax.config.TransformerAlgorithmMapper;
import org.apache.xml.security.stax.ext.stax.XMLSecAttribute;
import org.apache.xml.security.stax.ext.stax.XMLSecEvent;
import org.apache.xml.security.stax.ext.stax.XMLSecNamespace;
import org.apache.xml.security.stax.ext.stax.XMLSecStartElement;
import org.apache.xml.security.stax.impl.util.ConcreteLSInput;
import org.apache.xml.security.stax.securityEvent.DefaultTokenSecurityEvent;
import org.apache.xml.security.stax.securityEvent.EncryptedKeyTokenSecurityEvent;
import org.apache.xml.security.stax.securityEvent.KeyNameTokenSecurityEvent;
import org.apache.xml.security.stax.securityEvent.KeyValueTokenSecurityEvent;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityEvent.X509TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.InboundSecurityToken;
import org.apache.xml.security.stax.securityToken.SecurityTokenConstants;
import org.apache.xml.security.utils.ClassLoaderUtils;
import org.apache.xml.security.utils.JavaUtils;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 */
public class XMLSecurityUtils {

    private static final int MAX_SYMMETRIC_KEY_SIZE = 1024;

    protected XMLSecurityUtils() {
    }

    /**
     * Returns the Id reference without the leading #
     *
     * @param reference The reference on which to drop the #
     * @return The reference without a leading #
     */
    public static String dropReferenceMarker(String reference) {
        if (reference != null && reference.length() > 0 && reference.charAt(0) == '#') {
            return reference.substring(1);
        }
        return reference;
    }

    /**
     * Returns the XMLEvent type in String form
     *
     * @param xmlSecEvent
     * @return The XMLEvent type as string representation
     */
    public static String getXMLEventAsString(XMLSecEvent xmlSecEvent) {
        int eventType = xmlSecEvent.getEventType();

        switch (eventType) {
            case XMLStreamConstants.START_ELEMENT:
                return "START_ELEMENT";
            case XMLStreamConstants.END_ELEMENT:
                return "END_ELEMENT";
            case XMLStreamConstants.PROCESSING_INSTRUCTION:
                return "PROCESSING_INSTRUCTION";
            case XMLStreamConstants.CHARACTERS:
                return "CHARACTERS";
            case XMLStreamConstants.COMMENT:
                return "COMMENT";
            case XMLStreamConstants.START_DOCUMENT:
                return "START_DOCUMENT";
            case XMLStreamConstants.END_DOCUMENT:
                return "END_DOCUMENT";
            case XMLStreamConstants.ATTRIBUTE:
                return "ATTRIBUTE";
            case XMLStreamConstants.DTD:
                return "DTD";
            case XMLStreamConstants.NAMESPACE:
                return "NAMESPACE";
            default:
                throw new IllegalArgumentException("Illegal XMLSecEvent received: " + eventType);
        }
    }

    public static Transformer getTransformer(
            Transformer transformer, OutputStream outputStream, Map<String, Object> properties, String algorithm,
            XMLSecurityConstants.DIRECTION direction) throws XMLSecurityException {

        @SuppressWarnings("unchecked")
        Class<Transformer> transformerClass = (Class<Transformer>) TransformerAlgorithmMapper.getTransformerClass(algorithm, direction);
        Transformer childTransformer = null;

        try {
            childTransformer = JavaUtils.newInstanceWithEmptyConstructor(transformerClass);
            if (properties != null) {
                childTransformer.setProperties(properties);
            }
            if (outputStream != null) {
                childTransformer.setOutputStream(outputStream);
            } else {
                childTransformer.setTransformer(transformer);
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new XMLSecurityException(e);
        }
        return childTransformer;
    }

    @SuppressWarnings("unchecked")
    public static <T> T getQNameType(List<Object> objects, QName qName) {
        for (Object o : objects) {
            if (o instanceof JAXBElement) {
                JAXBElement<?> jaxbElement = (JAXBElement<?>) o;
                if (jaxbElement.getName().equals(qName)) {
                    return (T) jaxbElement.getValue();
                }
            }
        }
        return null;
    }

    public static String getQNameAttribute(Map<QName, String> attributes, QName qName) {
        return attributes.get(qName);
    }

    public static void createKeyValueTokenStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                    OutputProcessorChain outputProcessorChain, X509Certificate[] x509Certificates)
            throws XMLStreamException, XMLSecurityException {

        if (x509Certificates == null || x509Certificates.length == 0) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        X509Certificate x509Certificate = x509Certificates[0];
        PublicKey publicKey = x509Certificate.getPublicKey();
        createKeyValueTokenStructure(abstractOutputProcessor, outputProcessorChain, publicKey);
    }

    public static void createKeyValueTokenStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                    OutputProcessorChain outputProcessorChain,
                                                    PublicKey publicKey)
            throws XMLStreamException, XMLSecurityException {

        if (publicKey == null) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        String algorithm = publicKey.getAlgorithm();

        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyValue, true, null);

        if ("RSA".equals(algorithm)) {
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_RSAKeyValue, false, null);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Modulus, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(rsaPublicKey.getModulus().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Modulus);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Exponent, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(rsaPublicKey.getPublicExponent().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Exponent);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_RSAKeyValue);
        } else if ("DSA".equals(algorithm)) {
            DSAPublicKey dsaPublicKey = (DSAPublicKey) publicKey;
            BigInteger j = dsaPublicKey.getParams().getP().subtract(BigInteger.ONE).divide(dsaPublicKey.getParams().getQ());
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_DSAKeyValue, false, null);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_P, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(dsaPublicKey.getParams().getP().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_P);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Q, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(dsaPublicKey.getParams().getQ().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Q);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_G, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(dsaPublicKey.getParams().getG().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_G);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Y, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(dsaPublicKey.getY().toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_Y);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_J, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(j.toByteArray()));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_J);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_DSAKeyValue);
        } else if ("EC".equals(algorithm)) {
            ECPublicKey ecPublicKey = (ECPublicKey) publicKey;

            List<XMLSecAttribute> attributes = new ArrayList<>(1);
            attributes.add(abstractOutputProcessor.createAttribute(XMLSecurityConstants.ATT_NULL_URI, "urn:oid:" + ECDSAUtils.getOIDFromPublicKey(ecPublicKey)));
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_ECKeyValue, true, null);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_NamedCurve, false, attributes);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_NamedCurve);
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_PublicKey, false, null);
            abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(ECDSAUtils.encodePoint(ecPublicKey.getW(), ecPublicKey.getParams().getCurve())));
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_PublicKey);
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig11_ECKeyValue);
        }

        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyValue);
    }

    public static void createX509SubjectKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                               OutputProcessorChain outputProcessorChain,
                                                               X509Certificate[] x509Certificates)
                                                           throws XMLSecurityException, XMLStreamException {
        createX509SubjectKeyIdentifierStructure(abstractOutputProcessor, outputProcessorChain, x509Certificates, true);
    }

    public static void createX509SubjectKeyIdentifierStructure(AbstractOutputProcessor abstractOutputProcessor,
            OutputProcessorChain outputProcessorChain,
            X509Certificate[] x509Certificates,
            boolean outputX509Data)
        throws XMLSecurityException, XMLStreamException {
        if (x509Certificates == null || x509Certificates.length == 0) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        // SKI can only be used for a V3 certificate
        int version = x509Certificates[0].getVersion();
        if (version != 3) {
            throw new XMLSecurityException("certificate.noSki.lowVersion",
                                           new Object[]{version});
        }

        if (outputX509Data) {
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data, true, null);
        }

        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SKI, false, null);
        byte[] data = XMLX509SKI.getSKIBytesFromCert(x509Certificates[0]);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(data));
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SKI);

        if (outputX509Data) {
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data);
        }
    }

    public static void createX509CertificateStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                      OutputProcessorChain outputProcessorChain,
                                                      X509Certificate[] x509Certificates)
                                                  throws XMLSecurityException, XMLStreamException {
        createX509CertificateStructure(abstractOutputProcessor, outputProcessorChain, x509Certificates, true);
    }

    public static void createX509CertificateStructure(AbstractOutputProcessor abstractOutputProcessor,
            OutputProcessorChain outputProcessorChain,
            X509Certificate[] x509Certificates,
            boolean outputX509Data)
        throws XMLSecurityException, XMLStreamException {

        if (x509Certificates == null || x509Certificates.length == 0) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        if (outputX509Data) {
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data, true, null);
        }

        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Certificate, false, null);
        byte[] data;
        try {
            data = x509Certificates[0].getEncoded();
        } catch (CertificateEncodingException e) {
            throw new XMLSecurityException(e);
        }
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, XMLUtils.encodeToString(data));
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Certificate);

        if (outputX509Data) {
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data);
        }
    }

    public static void createX509SubjectNameStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                      OutputProcessorChain outputProcessorChain,
                                                      X509Certificate[] x509Certificates)
                                                  throws XMLSecurityException, XMLStreamException {
        createX509SubjectNameStructure(abstractOutputProcessor, outputProcessorChain, x509Certificates, true);
    }

    public static void createX509SubjectNameStructure(AbstractOutputProcessor abstractOutputProcessor,
            OutputProcessorChain outputProcessorChain,
            X509Certificate[] x509Certificates,
            boolean outputX509Data)
        throws XMLSecurityException, XMLStreamException {

        if (x509Certificates == null || x509Certificates.length == 0) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        if (outputX509Data) {
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data, true, null);
        }

        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SubjectName, false, null);
        String subjectName = x509Certificates[0].getSubjectX500Principal().getName();
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, subjectName);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SubjectName);

        if (outputX509Data) {
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data);
        }
    }

    public static void createX509IssuerSerialStructure(AbstractOutputProcessor abstractOutputProcessor,
                                                       OutputProcessorChain outputProcessorChain, X509Certificate[] x509Certificates)
                                                       throws XMLStreamException, XMLSecurityException {
        createX509IssuerSerialStructure(abstractOutputProcessor, outputProcessorChain, x509Certificates, true);
    }

    public static void createX509IssuerSerialStructure(AbstractOutputProcessor abstractOutputProcessor,
            OutputProcessorChain outputProcessorChain, X509Certificate[] x509Certificates,
            boolean outputX509Data)
            throws XMLStreamException, XMLSecurityException {

        if (x509Certificates == null || x509Certificates.length == 0) {
            throw new XMLSecurityException("stax.signature.publicKeyOrCertificateMissing");
        }

        if (outputX509Data) {
            abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data, true, null);
        }
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509IssuerSerial, false, null);
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509IssuerName, false, null);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, x509Certificates[0].getIssuerX500Principal().getName());
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509IssuerName);
        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SerialNumber, false, null);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, x509Certificates[0].getSerialNumber().toString());
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509SerialNumber);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509IssuerSerial);
        if (outputX509Data) {
            abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_X509Data);
        }
    }

    @SuppressWarnings("unchecked")
    public static TokenSecurityEvent<? extends InboundSecurityToken> createTokenSecurityEvent(
            final InboundSecurityToken inboundSecurityToken, String correlationID) throws XMLSecurityException {

        SecurityTokenConstants.TokenType tokenType = inboundSecurityToken.getTokenType();

        TokenSecurityEvent tokenSecurityEvent = null;
        if (SecurityTokenConstants.X509V1Token.equals(tokenType)
                || SecurityTokenConstants.X509V3Token.equals(tokenType)
                || SecurityTokenConstants.X509Pkcs7Token.equals(tokenType)
                || SecurityTokenConstants.X509PkiPathV1Token.equals(tokenType)) {
            tokenSecurityEvent = new X509TokenSecurityEvent();
        } else if (SecurityTokenConstants.KeyValueToken.equals(tokenType)) {
            tokenSecurityEvent = new KeyValueTokenSecurityEvent();
        } else if (SecurityTokenConstants.KeyNameToken.equals(tokenType)) {
            tokenSecurityEvent = new KeyNameTokenSecurityEvent();
        } else if (SecurityTokenConstants.DefaultToken.equals(tokenType)) {
            tokenSecurityEvent = new DefaultTokenSecurityEvent();
        } else if (SecurityTokenConstants.EncryptedKeyToken.equals(tokenType)) {
            tokenSecurityEvent = new EncryptedKeyTokenSecurityEvent();
        } else {
            throw new XMLSecurityException("stax.unsupportedToken",
                                           new Object[]{tokenType});
        }
        tokenSecurityEvent.setSecurityToken(inboundSecurityToken);
        tokenSecurityEvent.setCorrelationID(correlationID);
        return tokenSecurityEvent;
    }

    public static Set<String> getExcC14NInclusiveNamespacePrefixes(XMLSecStartElement xmlSecStartElement, boolean excludeVisible) {

        if (xmlSecStartElement == null) {
            return Collections.emptySet();
        }
        Set<String> prefixes = new TreeSet<>();

        XMLSecStartElement parentXMXmlSecStartElement = xmlSecStartElement.getParentXMLSecStartElement();
        if (parentXMXmlSecStartElement != null) {
            List<XMLSecNamespace> onElementDeclaredNamespaces = parentXMXmlSecStartElement.getOnElementDeclaredNamespaces();
            List<XMLSecNamespace> xmlSecNamespaces = new ArrayList<>();
            parentXMXmlSecStartElement.getNamespacesFromCurrentScope(xmlSecNamespaces);
            xmlSecNamespaces = xmlSecNamespaces.subList(0, xmlSecNamespaces.size() - onElementDeclaredNamespaces.size());

            //reverse iteration -> From current element namespaces to parent namespaces
            for (int i = xmlSecNamespaces.size() - 1; i >= 0; i--) {
                XMLSecNamespace xmlSecNamespace = xmlSecNamespaces.get(i);
                String prefix = xmlSecNamespace.getPrefix();
                if (prefix == null || prefix.isEmpty()) {
                    prefixes.add("#default");
                } else {
                    prefixes.add(xmlSecNamespace.getPrefix());
                }
            }

            if (excludeVisible) {
                for (XMLSecNamespace xmlSecNamespace : onElementDeclaredNamespaces) {
                    String prefix = xmlSecNamespace.getPrefix();
                    if (prefix == null || prefix.isEmpty()) {
                        prefixes.remove("#default");
                    } else {
                        prefixes.remove(prefix);
                    }
                }
                if (xmlSecStartElement.getName().getPrefix() == null || xmlSecStartElement.getName().getPrefix().isEmpty()) {
                    prefixes.remove("#default");
                } else {
                    prefixes.remove(xmlSecStartElement.getName().getPrefix());
                }
            }
        }
        return prefixes;
    }

    public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
        int read = 0;
        byte[] buf = new byte[4096];
        while ((read = inputStream.read(buf)) != -1) {
            outputStream.write(buf, 0, read);
        }
    }

    /**
     * Convert the raw key bytes into a SecretKey object of type symEncAlgo.
     */
    public static SecretKey prepareSecretKey(String symEncAlgo, byte[] rawKey) {
        // Do an additional check on the keysize required by the encryption algorithm
        int size = 0;
        try {
            size = JCEMapper.getKeyLengthFromURI(symEncAlgo) / 8;
        } catch (Exception e) {
            // ignore - some unknown (to JCEMapper) encryption algorithm
            size = 0;
        }
        String keyAlgorithm = JCEMapper.getJCEKeyAlgorithmFromURI(symEncAlgo);
        SecretKeySpec keySpec;
        if (size > 0 && !symEncAlgo.endsWith("gcm") && !symEncAlgo.contains("hmac-")) {
            keySpec =
                new SecretKeySpec(
                    rawKey, 0, rawKey.length > size ? size : rawKey.length, keyAlgorithm
                );
        } else if (rawKey.length > MAX_SYMMETRIC_KEY_SIZE) {
            // Prevent a possible attack where a huge secret key is specified
            keySpec =
                new SecretKeySpec(
                    rawKey, 0, MAX_SYMMETRIC_KEY_SIZE, keyAlgorithm
                );
        } else {
            keySpec = new SecretKeySpec(rawKey, keyAlgorithm);
        }
        return keySpec;
    }

    public static Schema loadXMLSecuritySchemas() throws SAXException {
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        schemaFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        schemaFactory.setResourceResolver(new LSResourceResolver() {
            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                if ("http://www.w3.org/2001/XMLSchema.dtd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/XMLSchema.dtd", XMLSecurityConstants.class));
                    return concreteLSInput;
                } else if ("XMLSchema.dtd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/XMLSchema.dtd", XMLSecurityConstants.class));
                    return concreteLSInput;
                } else if ("datatypes.dtd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/datatypes.dtd", XMLSecurityConstants.class));
                    return concreteLSInput;
                } else if ("http://www.w3.org/TR/2002/REC-xmldsig-core-20020212/xmldsig-core-schema.xsd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/xmldsig-core-schema.xsd", XMLSecurityConstants.class));
                    return concreteLSInput;
                } else if ("http://www.w3.org/2001/xml.xsd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/xml.xsd", XMLSecurityConstants.class));
                    return concreteLSInput;
                } else if ("rsa-pss.xsd".equals(systemId)) {
                    ConcreteLSInput concreteLSInput = new ConcreteLSInput();
                    concreteLSInput.setByteStream(
                            ClassLoaderUtils.getResourceAsStream("bindings/schemas/rsa-pss.xsd", XMLSecurityConstants.class));
                    return concreteLSInput;
                }
                return null;
            }
        });
        Schema schema = schemaFactory.newSchema(
                new Source[]{
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/exc-c14n.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/xmldsig-core-schema.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/xop-include.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/xenc-schema.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/xenc-schema-11.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/xmldsig11-schema.xsd", XMLSecurityConstants.class)),
                        new StreamSource(ClassLoaderUtils.getResourceAsStream("bindings/schemas/rsa-pss.xsd", XMLSecurityConstants.class))
                }
                );
        return schema;
    }

    public static void createKeyNameTokenStructure(AbstractOutputProcessor abstractOutputProcessor, OutputProcessorChain outputProcessorChain, String keyName)
            throws XMLStreamException, XMLSecurityException {

        if (keyName == null || keyName.isEmpty()) {
            throw new XMLSecurityException("stax.signature.keyNameMissing");
        }

        abstractOutputProcessor.createStartElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyName, true, null);
        abstractOutputProcessor.createCharactersAndOutputAsEvent(outputProcessorChain, keyName);
        abstractOutputProcessor.createEndElementAndOutputAsEvent(outputProcessorChain, XMLSecurityConstants.TAG_dsig_KeyName);
    }
}

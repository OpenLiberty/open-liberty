/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.config.xml.internal.validator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.ibm.websphere.config.ConfigValidationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.config.xml.internal.ServerConfiguration;
import com.ibm.ws.config.xml.internal.XMLConfigConstants;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The configuration validator that is used when Liberty is embedded within
 * another product.
 * 
 * @since V8.5 feature XXXXXX.
 */
public class EmbeddedXMLConfigValidator implements XMLConfigValidator {

    /**
     * Class name to use for FFDC.
     */
    private final static String CLASS_NAME = EmbeddedXMLConfigValidator.class.getName();

    /**
     * Trace component to use for this class.
     */
    private static final TraceComponent tc =
                    Tr.register(EmbeddedXMLConfigValidator.class,
                                XMLConfigConstants.TR_GROUP,
                                XMLConfigConstants.NLS_PROPS);

    /**
     * The Liberty certificate that corresponds to the private key used to
     * generate embedder certificates.
     */
    private static final String LIBERTY_CERTIFICATE =
                    "-----BEGIN CERTIFICATE-----\n" +
                                    "MIIDqDCCApCgAwIBAgIEUMJaljANBgkqhkiG9w0BAQsFADCBlTELMAkGA1UEBhMCVVMxETAPBgNV\n" +
                                    "BAgTCE5ldyBZb3JrMQ8wDQYDVQQHEwZBcm1vbmsxEjAQBgNVBAoTCUlCTSBDb3JwLjEXMBUGA1UE\n" +
                                    "CxMOU29mdHdhcmUgR3JvdXAxNTAzBgNVBAMTLFdlYlNwaGVyZSBBcHBsaWNhdGlvbiBTZXJ2ZXIg\n" +
                                    "TGliZXJ0eSBQcm9maWxlMB4XDTEyMTIwNzIxMDczNFoXDTMyMTIwMjIxMDczNFowgZUxCzAJBgNV\n" +
                                    "BAYTAlVTMREwDwYDVQQIEwhOZXcgWW9yazEPMA0GA1UEBxMGQXJtb25rMRIwEAYDVQQKEwlJQk0g\n" +
                                    "Q29ycC4xFzAVBgNVBAsTDlNvZnR3YXJlIEdyb3VwMTUwMwYDVQQDEyxXZWJTcGhlcmUgQXBwbGlj\n" +
                                    "YXRpb24gU2VydmVyIExpYmVydHkgUHJvZmlsZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC\n" +
                                    "ggEBAJ8l5a67C3jNwuS9g0rYYJ3dDjnykECQGXgQ7sP5i9ixF0Gg6NYesjn6VUBhf8ziC/4R4yrf\n" +
                                    "lPID+C1nM9SsUQld5QyAjbboRCXbW6+oIofzQKzWUHQQavXOXkH3i765GlsuME2qHYT+H8SQ0S0Z\n" +
                                    "2ZMQGr8PXA8lzTSvExozx+oXRXaqG97cpfNDjVZVswxR9QL5h5GdZ7INtN6OcNiKalz5cF95G4Vv\n" +
                                    "L1sjtRkPaupNV7C09hnw+UzdPjmxmIOkw6BbS/J0gkE+NSDjQCt1O4EalCOy1ERKMZIb3QsKyYQv\n" +
                                    "ebaXCm7u3aEy/yszaCwIIldSjYjM15SUQw20L5vbn/UCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEA\n" +
                                    "eJICZmkkBYgMqcq17+GRTWaDvKkcmBdBaIi6DDuRM31FNI7AzB2uLX2vJzXrrxPW41YturXKAZf2\n" +
                                    "5uKbgZOikO8e3djjCUhiLYhIm4aTJxPlrh+MejaNAwAVeZBunNrZL9VI8jtU/a1Vd9bEdQ305yXW\n" +
                                    "zt5c5mfJB3Yrn0LmwYKiSfG2pERy0TVnCpNLM6iQ7O2lQLVXXwlxNthWyOavEqlK54LR1GoklhC4\n" +
                                    "k1r4d/5Cc2tjsoIi1y9gZj0qZptJCM2o1RtWf/xa+MgIavH+M/FqLzphvGOoxkPOqOfgpLPhM7bp\n" +
                                    "LM6xqhiqexE5Xxq0JiNaxDi5iVUoDDxXG8ZslA==\n" +
                                    "-----END CERTIFICATE-----\n";

    /**
     * The document builder factory.
     */
    private final DocumentBuilderFactory dbFactory =
                    DocumentBuilderFactory.newInstance();

    /**
     * The XML signature factory.
     */
    private final XMLSignatureFactory sigFactory = XMLSignatureFactory.getInstance();

    /**
     * Indicates whether an error message has been issued.
     */
    private boolean errorMsgIssued = false;

    /**
     * Class constructor.
     */
    protected EmbeddedXMLConfigValidator() {
        dbFactory.setNamespaceAware(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws ConfigValidationException
     */
    @Override
    public InputStream validateResource(InputStream configDocInputStream, String docLocation) throws ConfigValidationException {

        errorMsgIssued = false;
        InputStream returnInputStream = null;

        try {
            // Reads the input stream into an output stream where we can copy it
            byte[] bfr = new byte[4096];
            int len = 0;
            ByteArrayOutputStream bfrOutputStream = new ByteArrayOutputStream();
            while ((len = configDocInputStream.read(bfr, 0, bfr.length)) > -1)
                bfrOutputStream.write(bfr, 0, len);
            configDocInputStream.close();
            bfrOutputStream.close();

            // Makes two copies of the input stream (one for us and one for the
            // the configuration document parser)
            InputStream validatorInputStream =
                            new ByteArrayInputStream(bfrOutputStream.toByteArray());
            returnInputStream = new ByteArrayInputStream(bfrOutputStream.toByteArray());

            // Parses the document to be verified
            Document document = dbFactory.newDocumentBuilder().parse(validatorInputStream);

            // Verifies the signature
            verifyDocument(document, docLocation);
        }

        // An error occurred while attempting to verify the document signature
        catch (Throwable t) {
            if (!errorMsgIssued) {
                if (t instanceof SAXException)
                    printErrorMessage("error.configValidator.parseFailed",
                                      docLocation, t.getMessage());
                else if (t instanceof MarshalException)
                    printErrorMessage("error.configValidator.unmarshalFailed",
                                      docLocation, t.getMessage());
                else
                    printErrorMessage("error.configValidator.error",
                                      docLocation, t.getMessage());
            }

            FFDCFilter.processException(t, CLASS_NAME, "138", this, new Object[] { docLocation });

            returnInputStream = null;

            // Throw exception to terminate the server
            throw new ConfigValidationException("Configuration parsing encountered an invalid document", docLocation);
        }

        return returnInputStream;
    }

    /**
     * Verifies the specified document.
     * <p>
     * The document is considered to be valid if it contains a valid signature
     * that has been authorized by the Liberty development organization.
     * 
     * @param document The document to be verified.
     * @param docLocation The location of the configuration document. (This string
     *            cannot be <code>null</code> but may be set to "UNKNOWN".)
     * 
     * @throws Exception Thrown if an error occurs while validating the signature.
     */
    private void verifyDocument(Document document, String docLocation)
                    throws Exception {
        // Gets the signature element within the document
        NodeList nodeList = document.getElementsByTagNameNS(XMLSignature.XMLNS,
                                                            "Signature");
        // Document does not contain a signature element
        if (nodeList.getLength() == 0) {
            printErrorMessage("error.configValidator.signatureMissing", docLocation);
            throw new IllegalStateException(
                            "Unable to find the Signature element in " +
                                            docLocation);
        }

        // Gets Liberty public key from hard-coded certificate
        InputStream inStream = new ByteArrayInputStream(LIBERTY_CERTIFICATE.getBytes());
        CertificateFactory certFactory = CertificateFactory.getInstance("X509");
        X509Certificate libertyCertificate = (X509Certificate)
                        certFactory.generateCertificate(inStream);
        PublicKey libertyPublicKey = libertyCertificate.getPublicKey();

        // Unmarshals the XML signature
        ConfigKeySelector keySelector = new ConfigKeySelector();
        DOMValidateContext valContext = new DOMValidateContext(keySelector,
                        nodeList.item(0));
        XMLSignature signature = sigFactory.unmarshalXMLSignature(valContext);

        // Validates the signature
        boolean sigValid = signature.validate(valContext);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "verifyDocument():  sigValid = " + sigValid);

        // Signature passed core validation
        if (sigValid) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "verifyDocument():  Signature passed core validation");

            // Gets the KeyInfo from the signature
            KeyInfo keyInfo = signature.getKeyInfo();
            if (keyInfo == null) {
                printErrorMessage("error.configValidator.keyInfoMissing", docLocation);
                throw new IllegalStateException("Unable to find KeyInfo");
            }

            X509Data x509Data = null;
            X509Certificate cert = null;

            // Scans through elements contained in KeyInfo until X509Data 
            // found
            @SuppressWarnings("unchecked")
            List<XMLStructure> keyInfoContent = keyInfo.getContent();
            for (XMLStructure xmlStructure : keyInfoContent) {
                if (x509Data != null) {
                    break;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "verifyDocument():  xmlStructure = " + xmlStructure);

                // X509Data element found
                if (xmlStructure instanceof X509Data) {

                    // Scans through elements contained in X509Data until 
                    // X509Certificate found
                    x509Data = (X509Data) xmlStructure;
                    for (Object obj : x509Data.getContent()) {
                        if (cert != null) {
                            break;
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            Tr.debug(tc, "verifyDocument():  obj = " + obj);

                        // X509Certificate found
                        if (obj instanceof X509Certificate) {

                            // Gets document signer public key from certificate 
                            cert = (X509Certificate) obj;
                            PublicKey signerPublicKey = cert.getPublicKey();

                            // Document was signed by Liberty organization
                            if (signerPublicKey.equals(libertyPublicKey)) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "Document signed by Liberty organization",
                                             docLocation);
                                Tr.info(tc, "info.configValidator.documentValid", docLocation);
                            }

                            // Document was not signed by Liberty organization
                            else {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    Tr.debug(tc, "verifyDocument():  Document was not signed by Liberty organization",
                                             docLocation);

                                // Checks if signer certificate was issued by Liberty organization
                                try {
                                    cert.verify(libertyPublicKey);
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        Tr.debug(tc, "verifyDocument():  Liberty organization is the CA for document signer",
                                                 docLocation);
                                    Tr.info(tc, "info.configValidator.documentValid", docLocation);
                                }

                                // Signer certificate was not issued by Liberty organization
                                catch (Throwable t) {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        Tr.debug(tc, "verifyDocument():  Liberty organization is not the CA for document signer",
                                                 docLocation);
                                    printErrorMessage("error.configValidator.signerNotAuthorized",
                                                      docLocation);
                                    FFDCFilter.processException(t, CLASS_NAME, "282", this,
                                                                new Object[] { docLocation });
                                    throw new IllegalStateException("Signer certificate not issued by Liberty", t);
                                }
                            }
                        }
                    }
                }
            }

            // X509Data element not found
            if (x509Data == null) {
                printErrorMessage("error.configValidator.x509DataMissing",
                                  docLocation);
                throw new IllegalStateException("Unable to find X509Data");
            }

            // X509Certificate element not found
            if (cert == null) {
                printErrorMessage("error.configValidator.x509CertificateMissing",
                                  docLocation);
                throw new IllegalStateException("Unable to find X509Certificate");
            }
        }

        // Signature failed core validation
        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "verifyDocument():  Signature failed core validation",
                         docLocation);
            sigValid = signature.getSignatureValue().validate(valContext);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "verifyDocument():  sigValid = " + sigValid);

            // Signature is valid - protected section of document has been 
            // modified
            if (sigValid) {
                printErrorMessage("error.configValidator.protectedSectionModified",
                                  docLocation);
                throw new IllegalStateException("Protectioned section of config document modified");
            }

            // Signature is not valid
            else {
                printErrorMessage("error.configValidator.signatureNotValid",
                                  docLocation);
                throw new IllegalStateException("Config document contains invalid signature");
            }
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @throws ConfigValidationException
     */
    @Override
    public void validateConfig(ServerConfiguration configuration) throws ConfigValidationException {

        // Default is that drop-ins are enabled
        boolean dropinsEnabled = configuration.isDropinsEnabled();

        // Drop-ins are enabled
        if (dropinsEnabled) {
            Tr.fatal(tc, "fatal.configValidator.dropinsEnabled");

            // Throw exception to terminate the server
            throw new ConfigValidationException("Drop-ins enabled in embedded environment");
        }

    }

    /**
     * Prints the specified error message.
     * 
     * @param key The resource bundle key for the message.
     * @param substitutions The values to be substituted for the tokens in the
     *            message skeleton.
     */
    private void printErrorMessage(String key, Object... substitutions) {
        Tr.error(tc, key, substitutions);
        errorMsgIssued = true;
    }
}

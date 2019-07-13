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

package org.opensaml.xml.security.x509;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.CRLException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;

import org.opensaml.xml.security.SecurityException;
import org.opensaml.xml.security.SecurityHelper;
import org.opensaml.xml.util.DatatypeHelper;
import org.opensaml.xml.util.IPAddressHelper;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Utility class for working with X509 objects.
 */
public class X509Util {
    private static TraceComponent tc = Tr.register(X509Util.class,
                                                   "SAML20",
                                                   "com.ibm.ws.security.saml.sso20.internal.resources.SamlSso20Messages");

    /** Encoding used to store a key or certificate in a file. */
    public static enum ENCODING_FORMAT {
        PEM, DER
    };

    /** Common Name (CN) OID. */
    public static final String CN_OID = "2.5.4.3";

    static final String OID_SUBJECTKEYIDENTIFIER = "2.5.29.14";

    /** RFC 2459 Other Subject Alt Name type. */
    public static final Integer OTHER_ALT_NAME = Integer.valueOf(0);

    /** RFC 2459 RFC 822 (email address) Subject Alt Name type. */
    public static final Integer RFC822_ALT_NAME = Integer.valueOf(1);

    /** RFC 2459 DNS Subject Alt Name type. */
    public static final Integer DNS_ALT_NAME = Integer.valueOf(2);

    /** RFC 2459 X.400 Address Subject Alt Name type. */
    public static final Integer X400ADDRESS_ALT_NAME = Integer.valueOf(3);

    /** RFC 2459 Directory Name Subject Alt Name type. */
    public static final Integer DIRECTORY_ALT_NAME = Integer.valueOf(4);

    /** RFC 2459 EDI Party Name Subject Alt Name type. */
    public static final Integer EDI_PARTY_ALT_NAME = Integer.valueOf(5);

    /** RFC 2459 URI Subject Alt Name type. */
    public static final Integer URI_ALT_NAME = Integer.valueOf(6);

    /** RFC 2459 IP Address Subject Alt Name type. */
    public static final Integer IP_ADDRESS_ALT_NAME = Integer.valueOf(7);

    /** RFC 2459 Registered ID Subject Alt Name type. */
    public static final Integer REGISTERED_ID_ALT_NAME = Integer.valueOf(8);

    /** Constructed. */
    protected X509Util() {

    }

    /**
     * Determines the certificate, from the collection, associated with the private key.
     * 
     * @param certs certificates to check
     * @param privateKey entity's private key
     * 
     * @return the certificate associated with entity's private key or null if not certificate in the collection is
     *         associated with the given private key
     * 
     * @throws SecurityException thrown if the public or private keys checked are of an unsupported type
     * 
     * @since 1.2
     */
    public static X509Certificate determineEntityCertificate(Collection<X509Certificate> certs, PrivateKey privateKey)
                    throws SecurityException {
        if (certs == null || privateKey == null) {
            return null;
        }

        for (X509Certificate certificate : certs) {
            if (SecurityHelper.matchKeyPair(certificate.getPublicKey(), privateKey)) {
                return certificate;
            }
        }

        return null;
    }

    /**
     * Gets the commons names that appear within the given distinguished name. The returned list provides the names in
     * the order they appeared in the DN.
     * 
     * @param dn the DN to extract the common names from
     * 
     * @return the common names that appear in the DN in the order they appear or null if the given DN is null
     */
    public static List<String> getCommonNames(X500Principal dn) {
        if (dn == null) {
            return null;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Extracting CNs from the following DN: {}", dn.toString());
        }
        List<String> commonNames = null;// new LinkedList<String>();
        try {
            Tr.error(tc, "SAML20_SERVER_INTERNAL_ERROR", "org.opensaml.xml.security.x509.X509Util->getCommonNames is not implemented yet");
            throw new Exception("TracingOnly");
        } catch (Exception e) { //IOException
            // TODO Error handling
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to extract common names from DN: ASN.1 parsing failed: " + e);
            }
            return commonNames;
        }
    }

    /**
     * Gets the list of alternative names of a given name type.
     * 
     * @param certificate the certificate to extract the alternative names from
     * @param nameTypes the name types
     * 
     * @return the alt names, of the given type, within the cert
     */
    public static List getAltNames(X509Certificate certificate, Integer[] nameTypes) {
        if (certificate == null) {
            return null;
        }

        List<Object> names = new LinkedList<Object>();
        Collection<List<?>> altNames = null;
        try {
            altNames = certificate.getSubjectAlternativeNames();
        } catch (CertificateParsingException e) {
            // TODO error handling
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Encountered an problem trying to extract Subject Alternate "
                             + "Name from supplied certificate: " + e);
            }
            return names;
        }

        if (altNames != null) {
            // 0th position represents the alt name type
            // 1st position contains the alt name data
            for (List altName : altNames) {
                for (Integer nameType : nameTypes) {
                    if (altName.get(0).equals(nameType)) {
                        names.add(convertAltNameType(nameType, altName.get(1)));
                        break;
                    }
                }
            }
        }

        return names;
    }

    /**
     * Gets the common name components of the issuer and all the subject alt names of a given type.
     * 
     * @param certificate certificate to extract names from
     * @param altNameTypes type of alt names to extract
     * 
     * @return list of subject names in the certificate
     */
    @SuppressWarnings("unchecked")
    public static List getSubjectNames(X509Certificate certificate, Integer[] altNameTypes) {
        List issuerNames = new LinkedList();

        List<String> entityCertCNs = X509Util.getCommonNames(certificate.getSubjectX500Principal());
        issuerNames.add(entityCertCNs.get(0));
        issuerNames.addAll(X509Util.getAltNames(certificate, altNameTypes));

        return issuerNames;
    }

    /**
     * Get the plain (non-DER encoded) value of the Subject Key Identifier extension of an X.509 certificate, if
     * present.
     * 
     * @param certificate an X.509 certificate possibly containing a subject key identifier
     * @return the plain (non-DER encoded) value of the Subject Key Identifier extension, or null if the certificate
     *         does not contain the extension
     * @throws IOException
     */
    public static byte[] getSubjectKeyIdentifier(X509Certificate cert) {
        byte[] ki = null;
        byte[] der = cert.getExtensionValue(OID_SUBJECTKEYIDENTIFIER);
        if (der != null) {
            ki = new byte[der.length - 4];
            System.arraycopy(der, 4, ki, 0, der.length - 4);
        }

        return (ki);
    }

    /**
     * Decodes X.509 certificates in DER or PEM format.
     * 
     * @param certs encoded certs
     * 
     * @return decoded certs
     * 
     * @throws CertificateException thrown if the certificates can not be decoded
     * 
     * @since 1.2
     */
    public static Collection<X509Certificate> decodeCertificate(File certs) throws CertificateException {
        if (!certs.exists()) {
            throw new CertificateException("Certificate file " + certs.getAbsolutePath() + " does not exist");
        }

        if (!certs.canRead()) {
            throw new CertificateException("Certificate file " + certs.getAbsolutePath() + " is not readable");
        }

        try {
            return decodeCertificate(DatatypeHelper.fileToByteArray(certs));
        } catch (IOException e) {
            throw new CertificateException("Error reading certificate file " + certs.getAbsolutePath(), e);
        }
    }

    /**
     * Decodes X.509 certificates in DER or PEM format.
     * 
     * @param certs encoded certs
     * 
     * @return decoded certs
     * 
     * @throws CertificateException thrown if the certificates can not be decoded
     */
    @SuppressWarnings("unchecked")
    public static Collection<X509Certificate> decodeCertificate(byte[] certs) throws CertificateException {
        if (certs == null || certs.length == 0) {
            return null;
        }
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        InputStream inStream = new ByteArrayInputStream(certs); // decode the value
        Collection<X509Certificate> x509Certs = (Collection<X509Certificate>) cf.generateCertificates(inStream);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getX509Certificate", x509Certs);
        }
        return x509Certs;
    }

    /**
     * Decodes CRLS in DER or PKCS#7 format. If in PKCS#7 format only the CRLs are decode, the rest of the content is
     * ignored.
     * 
     * @param crls encoded CRLs
     * 
     * @return decoded CRLs
     * 
     * @throws CRLException thrown if the CRLs can not be decoded
     * 
     * @since 1.2
     */
    public static Collection<X509CRL> decodeCRLs(File crls) throws CRLException {
        if (!crls.exists()) {
            throw new CRLException("CRL file " + crls.getAbsolutePath() + " does not exist");
        }

        if (!crls.canRead()) {
            throw new CRLException("CRL file " + crls.getAbsolutePath() + " is not readable");
        }

        try {
            return decodeCRLs(DatatypeHelper.fileToByteArray(crls));
        } catch (IOException e) {
            throw new CRLException("Error reading CRL file " + crls.getAbsolutePath(), e);
        }
    }

    /**
     * Decodes CRLS in DER or PKCS#7 format. If in PKCS#7 format only the CRLs are decode, the rest of the content is
     * ignored.
     * 
     * @param crls encoded CRLs
     * 
     * @return decoded CRLs
     * 
     * @throws CRLException thrown if the CRLs can not be decoded
     */
    @SuppressWarnings("unchecked")
    public static Collection<X509CRL> decodeCRLs(byte[] crls) throws CRLException {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (Collection<X509CRL>) cf.generateCRLs(new ByteArrayInputStream(crls));
        } catch (GeneralSecurityException e) {
            throw new CRLException("Unable to decode X.509 certificates");
        }
    }

    /**
     * Gets a formatted string representing identifier information from the supplied credential.
     * 
     * <p>
     * This could for example be used in logging messages.
     * </p>
     * 
     * <p>
     * Often it will be the case that a given credential that is being evaluated will NOT have a value for the entity ID
     * property. So extract the certificate subject DN, and if present, the credential's entity ID.
     * </p>
     * 
     * @param credential the credential for which to produce a token.
     * @param handler the X.500 DN handler to use. If null, a new instance of {@link InternalX500DNHandler} will be
     *            used.
     * 
     * @return a formatted string containing identifier information present in the credential
     */
    public static String getIdentifiersToken(X509Credential credential, X500DNHandler handler) {
        X500DNHandler x500DNHandler;
        if (handler != null) {
            x500DNHandler = handler;
        } else {
            x500DNHandler = new InternalX500DNHandler();
        }
        X500Principal x500Principal = credential.getEntityCertificate().getSubjectX500Principal();
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(String.format("subjectName='%s'", x500DNHandler.getName(x500Principal)));
        if (!DatatypeHelper.isEmpty(credential.getEntityId())) {
            builder.append(String.format(" |credential entityID='%s'", DatatypeHelper.safeTrimOrNullString(credential
                            .getEntityId())));
        }
        builder.append(']');
        return builder.toString();
    }

    /**
     * Convert types returned by Bouncy Castle X509ExtensionUtil.getSubjectAlternativeNames(X509Certificate) to be
     * consistent with what is documented for: java.security.cert.X509Certificate#getSubjectAlternativeNames.
     * 
     * @param nameType the alt name type
     * @param nameValue the alt name value
     * @return converted representation of name value, based on type
     */
    private static Object convertAltNameType(Integer nameType, Object nameValue) {
        if (DIRECTORY_ALT_NAME.equals(nameType) || DNS_ALT_NAME.equals(nameType) || RFC822_ALT_NAME.equals(nameType)
            || URI_ALT_NAME.equals(nameType) || REGISTERED_ID_ALT_NAME.equals(nameType)) {

            // these are just strings in the appropriate format already, return as-is
            return nameValue;
        }

        if (IP_ADDRESS_ALT_NAME.equals(nameType)) {
            // this is a byte[], IP addr in network byte order
            return IPAddressHelper.addressToString((byte[]) nameValue);
        }

        if (EDI_PARTY_ALT_NAME.equals(nameType) || X400ADDRESS_ALT_NAME.equals(nameType)
            || OTHER_ALT_NAME.equals(nameType)) {

            // these have no defined representation, just return a DER-encoded byte[]
            //return ((DERObject) nameValue).getDEREncoded();
            Tr.error(tc, "SAML20_SERVER_INTERNAL_ERROR", "org.opensaml.xml.security.x509.X509Util->convertAltNameType(392) is not implemented yet");
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Encountered unknown alt name type '{}', adding as-is", nameType);
        }
        return nameValue;
    }

}
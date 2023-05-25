/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package componenttest.security.utils;

import java.io.File;
import java.io.FileWriter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import com.ibm.websphere.simplicity.log.Log;

/**
 * Util class for generating SSL certificates and keys for testing SSL or TLS connections in fat tests.
 * <p>
 * An example of using this class to generate a certificate file and private key file for a remote docker container:
 *
 * <pre>
 * private static boolean createdSSLFiles = false;
 *
 * private synchronized static void generateSSLFiles() {
 *     if (createdSSLFiles) {
 *         return;
 *     }
 *
 *     try {
 *         KeyPair generatedKeyPair = SSLUtils.generateKeyPair();
 *
 *         String dockerIP = DockerClientFactory.instance().dockerHostIpAddress();
 *         String dnName = "O=Evil Inc Test Certificate, CN=" + dockerIP + ", L=Toronto,C=CA";
 *         List<String> genericNameList = new ArrayList<String>();
 *         genericNameList.add(dockerIP);
 *
 *         Certificate certificateObject = SSLUtils.selfSign(generatedKeyPair, dnName, genericNameList);
 *
 *         String pathToPrivateKey = server.getServerSharedPath() + "/private.key";
 *         File privateKeyFile = new File(pathToPrivateKey);
 *         SSLUtils.exportPrivateKeyToFile(privateKeyFile, generatedKeyPair);
 *
 *         String pathToCertificate = server.getServerSharedPath() + "/certificate.crt";
 *         File certificateFile = new File(pathToCertificate);
 *         SSLUtils.exportCertificateToFile(certificateFile, certificateObject);
 *         createdSSLFiles = true;
 *     } catch (Exception e) { //If we get an exception let the test fail and show the developer what went wrong
 *         throw new RuntimeException("Exception doing SSLFiles", e);
 *     }
 * }
 * </pre>
 */
public class SSLUtils {

    private static final Class<?> c = SSLUtils.class;
    private static SecureRandom secureRandom = null;

    private static synchronized SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        return secureRandom;
    }

    /**
     * Creates a new KeyPair
     *
     * @Return a KeyPair
     */
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, getSecureRandom());
        KeyPair pair = generator.generateKeyPair();

        return pair;
    }

    //Adapted from https://stackoverflow.com/a/43918337 and https://stackoverflow.com/a/44437586
    /**
     * Creates a new public key certificate
     *
     * @Param  keyPair the public key from this keyPair will be added to the certificate.
     * @Param  subjectDN the certificate's subjectDN
     * @Param  genericNameList a list of IPv4 addresses or hostnames that will be added to the certificate under Subject Alternative Name. This may be empty or null.
     * @Return a Certificate
     */
    public static Certificate selfSign(KeyPair keyPair, String subjectDN, List<String> genericNameList) throws Exception {
        Log.info(c, "selfSign", "entering SSLUtils.selfSign");

        final String DNS_PATTERN = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)*([A-Za-z0-9]|[A-Za-z0-9][A-Za-z0-9\\-]*[A-Za-z0-9])$";//from https://stackoverflow.com/a/106223
        final String IPV4_PATTERN = "^((25[0-5]|(2[0-4]|1[0-9]|[1-9]|)[0-9])(\\.(?!$)|$)){4}$";//from https://stackoverflow.com/a/36760050

        Provider bcProvider = new BouncyCastleProvider();
        Security.addProvider(bcProvider);

        long now = System.currentTimeMillis();
        Date startDate = new Date(now);

        X500Name dnName = new X500Name(subjectDN);

        // Using the current timestamp as the certificate serial number
        BigInteger certSerialNumber = new BigInteger(Long.toString(now));

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(startDate);
        // 1 Yr validity
        calendar.add(Calendar.YEAR, 1);

        Date endDate = calendar.getTime();

        String signatureAlgorithm = "SHA256WithRSA";

        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(keyPair
                        .getPublic()
                        .getEncoded());

        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(dnName, certSerialNumber, startDate, endDate, dnName, subjectPublicKeyInfo);

        if (genericNameList != null && !genericNameList.isEmpty()) {
            List<GeneralName> altNames = new ArrayList<GeneralName>();
            for (String name : genericNameList) {
                if (name.matches(IPV4_PATTERN)) {
                    Log.info(c, "selfSign", "adding ip GeneralName " + name);
                    altNames.add(new GeneralName(GeneralName.iPAddress, name));
                } else if (name.matches(DNS_PATTERN)) {
                    Log.info(c, "selfSign", "adding dns GeneralName " + name);
                    altNames.add(new GeneralName(GeneralName.dNSName, name));
                } else {
                    Log.info(c, "selfSign", "" + name + " is not a valid IPv4 address or DNS hostname");
                }
            }

            GeneralNames subjectAltNames = GeneralNames.getInstance(new DERSequence(altNames.toArray(new GeneralName[] {})));
            certificateBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltNames);
        }

        ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).setProvider(
                                                                                                  bcProvider)
                        .build(keyPair.getPrivate());

        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        Certificate selfSignedCert = new JcaX509CertificateConverter()
                        .getCertificate(certificateHolder);

        return selfSignedCert;
    }

    /**
     * Exports a public key to a file in PEM format.
     *
     * @Param outputFile the destination file. Parent directories will be created and existing files will be overwritten.
     * @Param keyPair the public key from this keyPair will be exported
     */
    public static void exportPublicKeyToFile(File outputFile, KeyPair keyPair) throws Exception {
        Log.info(c, "exportPublicKeyToFile", "entering SSLUtils.exportPublicKeyToFile");

        PublicKey publicKey = keyPair.getPublic();
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();

        FileWriter fw = new FileWriter(outputFile, false);
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(fw)) { //pemWriter is a BufferedWriter so pemWriter.close() also closes fw
            pemWriter.writeObject(publicKey);
        }

        Log.info(c, "exportPublicKeyToFile", "exported to  " + outputFile);
    }

    /**
     * Exports a private key to a file in PEM format.
     *
     * @Param outputFile the destination file. Parent directories will be created and existing files will be overwritten.
     * @Param keyPair the private key from this keyPair will be exported
     */
    public static void exportPrivateKeyToFile(File outputFile, KeyPair keyPair) throws Exception {
        Log.info(c, "exportPrivateKeyToFile", "entering SSLUtils.exportPrivateKeyToFile");

        PrivateKey privateKey = keyPair.getPrivate();
        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();

        FileWriter fw = new FileWriter(outputFile, false);
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(fw)) {
            pemWriter.writeObject(privateKey);
        }

        Log.info(c, "exportPrivateKeyToFile", "exported to  " + outputFile);
    }

    /**
     * Exports a private key to a file in PEM format.
     *
     * @Param outputFile the destination file. Parent directories will be created and existing files will be overwritten.
     * @Param cert the certificate to be exported
     */
    public static void exportCertificateToFile(File outputFile, Certificate cert) throws Exception {

        Log.info(c, "exportCertificateToFile", "entering SSLUtils.exportCertificateToFile");

        outputFile.getParentFile().mkdirs();
        outputFile.createNewFile();

        FileWriter fw = new FileWriter(outputFile, false);
        try (JcaPEMWriter pemWriter = new JcaPEMWriter(fw)) {
            pemWriter.writeObject(cert);
        }

        Log.info(c, "exportCertificateToFile", "exported to  " + outputFile);
    }

}

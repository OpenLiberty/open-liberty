/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.utility.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import java.util.Arrays;
import java.util.Base64;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

import com.ibm.websphere.simplicity.LocalFile;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(FATRunner.class)
public class SecurityUtilityImportSignerTest {
    private static LibertyServer server = LibertyServerFactory.getLibertyServer("PasswordUtilityEncodeTest");
    private static final String BEGIN_PEM = "-----BEGIN CERTIFICATE-----";
    private static final String END_PEM = "-----END CERTIFICATE-----";

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
        server.waitForStringInLogUsingMark(".*CWPKI0803A.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void importSelfSignedAsPemFile() throws Exception {
        File f = new File("importSelfSignedAsPemFile.pem");
        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept");

        validatePEM(f);
    }

    @Test
    public void importSelfSignedAsPemConditionalCorrectSHA1() throws Exception {
        CharSequence hash = getExpectedHash("SHA-1");

        File f = new File("importSelfSignedAsPemConditionalCorrectSHA1.pem");

        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept=" + hash);

        validatePEM(f);
    }

    @Test
    public void importSelfSignedAsPemConditionalCorrectSHA256() throws Exception {
        CharSequence hash = getExpectedHash("SHA-256");

        File f = new File("importSelfSignedAsPemConditionalCorrectSHA256.pem");

        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept=" + hash);

        validatePEM(f);
    }

    @Test
    public void importSelfSignedAsPemConditionalInCorrectSHA1() throws Exception {
        CharSequence hash = "ab:a8:fd:e8:3c:6f:47:97:db:97:88:36:9e:a1:fd:f7:64:54:51:fb";

        File f = new File("importSelfSignedAsPemConditionalInCorrectSHA1.pem");

        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept=" + hash);

        assertFalse("The pem file should not exist: " + f, f.exists());
    }

    @Test
    public void importSelfSignedAsPemConditionalInCorrectSHA256() throws Exception {
        CharSequence hash = "8d:b0:14:a4:6b:cf:aa:5f:47:b0:b5:cc:42:ed:4a:32:ab:44:82:b5:5f:5e:2c:46:42:7e:3f:fe:53:44:7e:61";

        File f = new File("importSelfSignedAsPemConditionalInCorrectSHA256.pem");

        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept=" + hash);

        assertFalse("The pem file should not exist: " + f, f.exists());
    }

    @Test
    public void importSelfSignedAsPKCSFile() throws Exception {
        File f = new File("importSelfSignedAsPKCSFile.p12");
        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept", "--password=WebAS");

        KeyStore ks = validateKeyStore(f, "PKCS12");

        validateTLSConnection(ks, "localhost", server.getHttpDefaultSecurePort());
    }

    @Test
    public void importSelfSignedAsJSKFile() throws Exception {
        File f = new File("importSelfSignedAsJSKFile.jks");
        SecurityUtilityScriptUtils.execute(null, "importSigner", "--host=localhost", "--port=" + server.getHttpDefaultSecurePort(), "--keyStore=" + f.getName(), "--accept", "--password=WebAS");

        validateKeyStore(f, "JKS");
    }

    private static CharSequence getExpectedHash(String hash) throws Exception {
        LocalFile lf = server.copyFileToTempDir("resources/security/key.p12", "key.p12");

        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(lf.openForReading(), "AReallySecureSecret".toCharArray());

        X509Certificate cert = null;
        Enumeration<String> entries = ks.aliases();
        while (entries.hasMoreElements()) {
            String entry = entries.nextElement();
            Certificate c = ks.getCertificate(entry);
            if (c != null) {
                cert = (X509Certificate) c;
                break;
            }
        }

        assertNotNull("Could not find certificate", cert);

        MessageDigest digest = MessageDigest.getInstance(hash);

        return hash(cert.getEncoded(), digest);
    }

    private static String readFully(File f) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(f));
        StringBuilder builder = new StringBuilder();

        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append("\r\n");
        }

        return builder.toString();
    }


    public static void validateTLSConnection(KeyStore ks, String host, int port) throws Exception{
        SSLContext ctx = SSLContext.getInstance("TLS");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        ctx.init(null, tmf.getTrustManagers(), null);
        SSLSocket soc = (SSLSocket) ctx.getSocketFactory().createSocket(host, port);
        soc.startHandshake();
        soc.close();
    }

    private static void validatePEM(File f) throws Exception {
        assertTrue(f + " does not exist ", f.exists());

        String pemContent = readFully(f);

        assertTrue("First line in pem file wasn't the cert start eyecatcher", pemContent.startsWith(BEGIN_PEM + "\r\n"));
        assertTrue("Last line in pem file wasn't the cert end eyecatcher", pemContent.endsWith("\r\n" + END_PEM + "\r\n"));

        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate)factory.generateCertificate(new FileInputStream(f));

        assertEquals("The Subject DN is not as expected", "CN=localhost,OU=PasswordUtilityEncodeTest,O=ibm,C=us", cert.getSubjectX500Principal().getName());

    }

    private static KeyStore validateKeyStore(File f, String type) throws Exception {
        assertTrue(f + " does not exist ", f.exists());

        KeyStore ks = KeyStore.getInstance(type);
        ks.load(new FileInputStream(f), "WebAS".toCharArray());

        Enumeration<String> entries = ks.aliases();

        boolean once = false;
        while (entries.hasMoreElements()) {
            assertFalse("There are multiple entires in the keystore", once);
            once = true;
            String entry = entries.nextElement();
            assertTrue("The entry is not a certificate entry", ks.isCertificateEntry(entry));
            X509Certificate cert = (X509Certificate)ks.getCertificate(entry);
            assertEquals("The Subject DN is not as expected", "CN=localhost,OU=PasswordUtilityEncodeTest,O=ibm,C=us", cert.getSubjectX500Principal().getName());
        }

        return ks;
    }

    private static CharSequence hash(byte[] encoded, MessageDigest hasher) {
        byte[] hash = hasher.digest(encoded);

        return toHex(hash);
    }

    private static CharSequence toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();

        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
            builder.append(':');
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder;
    }
}
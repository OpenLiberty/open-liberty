/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ssl.fat.pkcs12;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.rules.TestName;

import com.ibm.websphere.simplicity.Machine;
import com.ibm.websphere.simplicity.ProgramOutput;
import com.ibm.websphere.simplicity.RemoteFile;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.webcontainer.security.test.servlets.SSLBasicAuthClient;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

@Ignore("This is not a test")
public abstract class CommonSSLTest {
    private static final Class<?> c = CommonSSLTest.class;
    // These values are the names of the server.xml files in publish/files
    protected static final String DEFAULT_MINIMAL_SSL_CONFIG = "defaultMinimalSSLConfigServer.xml";
    protected static final String DEFAULT_MINIMAL_SSL_CONFIG_WITH_JKS_TYPE = "defaultMinimalSSLConfigServerWithJKSType.xml";
    protected static final String DEFAULT_MINIMAL_SSL_CONFIG_WITH_PKCS12_TYPE = "defaultMinimalSSLConfigServerWithPKCS12Type.xml";
    protected static final String DEFAULT_MINIMAL_WITH_APPSECURITY_SSL_CONFIG = "defaultMinimalSSLConfigWithAppSecurityServer.xml";
    protected static final String DEFAULT_MINIMAL_WITH_APPSECURITY_SSL_CONFIG_EBA = "defaultMinimalSSLConfigWithAppSecurityServerEBA.xml";
    protected static final String GEN_RELATIVE_PATH_MINIMAL_SSL_CONFIG = "genRelativePathMinimalSSLConfigServer.xml";
    protected static final String ABSOLUTE_PATH_MINIMAL_SSL_CONFIG = "absolutePathMinimalSSLConfigServer.xml";
    protected static final String RELATIVE_PATH_SSL_CONFIG = "relativePathSSLConfigServer.xml";
    protected static final String NON_DEFAULT_KEYSTORE_LOCATION_USING_JKS_NO_TYPE_SPECIFIED = "nonDefaultJKSKeystoreLocationConfigServer.xml";
    protected static final String NON_DEFAULT_KEYSTORE_LOCATION_USING_PKCS12_NO_TYPE_SPECIFIED = "nonDefaultPKCS12KeystoreLocationConfigServer.xml";
    protected static final String MAXIMAL_NO_TRUSTSTORE_SSL_CONFIG = "maximalNoTrusStoreSSLConfigServer.xml";
    protected static final String MAXIMAL_SSL_CONFIG = "maximalSSLConfigServer.xml";
    protected static final String MAXIMAL_SSL_CONFIG_WITH_WAB = "maximalSSLConfigServerWithWAB.xml";
    protected static final String OVERRIDE_DEFAULT_SSL_CONFIG = "overrideDefaultSSLConfigServer.xml";
    protected static final String OVERRIDE_DEFAULT_SSL_CONFIG_USING_DEFAULT_KEYSTORE_SSL_CONFIG = "overrideDefaultSSLConfigUsingDefaultKeyStoreServer.xml";
    protected static final String OVERRIDE_DEFAULT_SSL_CONFIG_WITH_DEFAULT_KEYSTORE_SSL_CONFIG = "overrideDefaultSSLConfigWithDefaultKeyStoreServer.xml";
    protected static final String KEYSTORE_FILE_DOESNT_EXIST_SSL_CONFIG = "keyStoreFileDoesNotExistSSLConfigServer.xml";
    protected static final String ONLY_TRUSTSTOREREF_SSL_CONFIG = "onlyTrustStoreSSLConfigServer.xml";
    protected static final String KEYSTORE_WITH_WRONG_PASSWORD_SSL_CONFIG = "keyStoreWithWrongPasswordSSLConfigServer.xml";
    protected static final String KEYSTORE_WITH_SHORT_PASSWORD_SSL_CONFIG = "keyStoreWithShortPasswordSSLConfigServer.xml";
    protected static final String NO_SSL_CONFIG_BUT_DOES_INCLUDE_SSL_FEATURE = "noSSLConfigServer.xml";
    protected static final String NO_SSL_CONFIG_BUT_HAS_SSL_FEATURE_AND_APPSECURITY_CONFIG = "noSSLConfigWithAppSecurityServer.xml";
    protected static final String NO_SSL_FEATURE_BUT_HAS_SERVLET = "noSSLFeatureServer.xml";
    protected static final String LDAP_WITHOUT_SSL = "ldapWithoutSSL.xml";
    protected static final String LDAP_WITH_SSL = "ldapWithSSL.xml";
    protected static final String LDAP_WITH_SSL_FILE_MONITOR = "ldapWithSSLFileMonitor.xml";
    protected static final String SSL_FILE_MONITOR_KEYSTORE = "SSLFileMonitor.xml";
    protected static final String SSL_FILE_MONITOR_KEYSTORE_MBEAN = "SSLFileMonitorMbean.xml";
    protected static final String SSL_CONFIG_CLIENT_AUTH = "SSLConfigClientAuth.xml";
    protected static final String SSL_CONFIG_KEY_PASSWORD = "SSLConfigKeyPassword.xml";
    protected static final String SSL_CONFIG_KEY_BAD_HWCFG = "SSLConfigBadHWCfg.xml";
    protected static final String SSL_CONFIG_OUTBOUND_SSL_DEFAULT = "SSLConfigOutboundSSLDefault.xml";
    protected static final String SSL_CONFIG_OUTBOUND_SSL_NO_TRANSPORT = "SSLConfigOutboundSSLNoTransport.xml";
    protected static final String SSL_CONFIG_OUTBOUND_SSL_DEFAULT_NO_TRUST = "SSLConfigOutboundSSLDefaultNoTrust.xml";
    protected static final String SSL_CONFIG_DYNAMIC_SSL_OUTBOUND = "SSLConfigDynamicOutbound.xml";
    protected static final String KEYSTORE_DEFAULT_NON_DEFAULT_LOC_PKCS12 = "defaultKeyStoreWithExistingPKCS12NonDefaultLocation.xml";
    protected static final String KEYSTORE_DEFAULT_NON_DEFAULT_LOC_JKS = "defaultKeyStoreWithExistingJKSNonDefaultLocation.xml";

    // These values are the keystore and truststore values of the server.xml files in publish/files
    protected static final String DEFAULT_GENERATED_KEY_PATH = "resources/security/key.p12";
    protected static final String DEFAULT_GENERATED_KEY_PASSWORD = "Liberty";
    protected static final String ALTERNATE_GENERATED_KEY_PATH = "resources/security/alternateKey.jks";
    protected static final String ALTERNATE_GENERATED_KEY_PASSWORD = "Liberty";
    protected static final String DEFAULT_PREGENERATED_KEY_PATH = "DefaultServerKeyFile.jks";
    protected static final String DEFAULT_PREGENERATED_KEY_PASSWORD = "Liberty";
    protected static final String DEFAULT_PREGENERATED_TRUST_PATH = "DefaultServerTrustFile.jks";
    protected static final String DEFAULT_PREGENERATED_TRUST_PASSWORD = "Liberty";
    protected static final String ALTERNATE_PREGENERATED_KEY_PATH = "AlternateServerKeyFile.jks";
    protected static final String ALTERNATE_PREGENERATED_KEY_PASSWORD = "ytrebiL";
    protected static final String ALTERNATE_PREGENERATED_TRUST_PATH = "AlternateServerTrustFile.jks";
    protected static final String ALTERNATE_PREGENERATED_TRUST_PASSWORD = "ytrebiL";
    protected static final String NON_DEFAULT_JKS_KEY_PATH = "myKeyStore.jks";
    protected static final String NON_DEFAULT_JKS_KEY_PASSWORD = "Liberty";
    protected static final String NON_DEFAULT_PKCS12_KEY_PATH = "myKeyStore.p12";
    protected static final String NON_DEFAULT_PKCS12_KEY_PASSWORD = "Liberty";

    // SSL protocols
    protected static final String TLS_PROTOCOL = "TLSv1";
    protected static final String TLSV11_PROTOCOL = "TLSv1.1";
    protected static final String TLSV12_PROTOCOL = "TLSv1.2";

    protected final LibertyServer server;
    private final String LOCAL_KEYSTORE_PATH;
    private final String LOCAL_TRUSTSTORE_PATH;

    @Rule
    public TestName name = new TestName();

    protected CommonSSLTest(LibertyServer server) {
        this.server = server;
        this.LOCAL_KEYSTORE_PATH = server.pathToAutoFVTTestFiles + "localKeyStore.tmp";
        this.LOCAL_TRUSTSTORE_PATH = server.pathToAutoFVTTestFiles + "localTrustStore.tmp";
    }

    @Before
    public void setUp() throws Exception {
        deleteFileIfExists(server, DEFAULT_GENERATED_KEY_PATH);
        deleteFileIfExists(server, ALTERNATE_GENERATED_KEY_PATH);
        assertFalse("Auto-generated file " + DEFAULT_GENERATED_KEY_PATH + " exists before the test, this is an invalid pre-condition we should not hit",
                    fileExists(server, DEFAULT_GENERATED_KEY_PATH));
        assertFalse("Auto-generated file " + ALTERNATE_GENERATED_KEY_PATH + " exists before the test, this is an invalid pre-condition we should not hit",
                    fileExists(server, ALTERNATE_GENERATED_KEY_PATH));
        LDAPUtils.addLDAPVariables(server);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWNEN0050W", "SRVE0272W");
        }
        deleteFileIfExists(server, DEFAULT_GENERATED_KEY_PATH);
        deleteFileIfExists(server, ALTERNATE_GENERATED_KEY_PATH);
    }

    /**
     * Creates an SSLBasicAuthClient with trust from the remote server keystore.
     *
     * @return
     * @throws Exception
     */
    protected SSLBasicAuthClient createSSLClientWithTrust(String serverTrustStorePath, String serverTrustStorePassword) throws Exception {
        return createSSLClientWithTrust(serverTrustStorePath, serverTrustStorePassword, null);
    }

    /**
     * Creates an SSLBasicAuthClient with trust from the remote server keystore.
     *
     * @return
     * @throws Exception
     */
    protected SSLBasicAuthClient createSSLClientWithTrust(String serverTrustStorePath, String serverTrustStorePassword, String sslProtocol) throws Exception {
        Log.info(c, "createSSLClientWithTrust", "Copying remote truststore: " + serverTrustStorePath);

        RemoteFile remoteTrustStore = server.getFileFromLibertyServerRoot(serverTrustStorePath);
        RemoteFile localTrustStore = new RemoteFile(Machine.getLocalMachine(), LOCAL_TRUSTSTORE_PATH);
        localTrustStore.copyFromSource(remoteTrustStore);
        assertTrue("Remote truststore did not copy to the local machine",
                   localTrustStore.exists());
        Log.info(c, "createSSLClientWithTrust", "Remote truststore successfully copied to " + localTrustStore);
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server, SSLBasicAuthClient.DEFAULT_REALM, SSLBasicAuthClient.DEFAULT_SERVLET_NAME, SSLBasicAuthClient.DEFAULT_CONTEXT_ROOT, null, null, localTrustStore
                        .getAbsolutePath(), serverTrustStorePassword, sslProtocol);
        return sslClient;
    }

    /**
     * Creates an SSLBasicAuthClient with trust from the remote server keystore.
     *
     * @return
     * @throws Exception
     */
    protected SSLBasicAuthClient createSSLClientWithKeyAndTrust(String serverKeyStorePath, String serverKeyStorePassword, String serverTrustStorePath,
                                                                String serverTrustStorePassword) throws Exception {
        return createSSLClientWithKeyAndTrust(serverKeyStorePath, serverKeyStorePassword, serverTrustStorePath, serverTrustStorePassword, null);
    }

    /**
     * Creates an SSLBasicAuthClient with trust from the remote server keystore.
     *
     * @return
     * @throws Exception
     */
    protected SSLBasicAuthClient createSSLClientWithKeyAndTrust(String serverKeyStorePath, String serverKeyStorePassword, String serverTrustStorePath,
                                                                String serverTrustStorePassword, String sslProtocol) throws Exception {
        Log.info(c, "createSSLClientWithTrust", "Copying remote truststore: " + serverTrustStorePath);
        Log.info(c, "createSSLClientWithTrust", "Copying remote keystore: " + serverKeyStorePath);
        RemoteFile remoteKeyStore = server.getFileFromLibertyServerRoot(serverKeyStorePath);
        RemoteFile localKeyStore = new RemoteFile(Machine.getLocalMachine(), LOCAL_KEYSTORE_PATH);
        localKeyStore.copyFromSource(remoteKeyStore);
        RemoteFile remoteTrustStore = server.getFileFromLibertyServerRoot(serverTrustStorePath);
        RemoteFile localTrustStore = new RemoteFile(Machine.getLocalMachine(), LOCAL_TRUSTSTORE_PATH);
        localTrustStore.copyFromSource(remoteTrustStore);
        assertTrue("Remote truststore did not copy to the local machine",
                   localTrustStore.exists());
        Log.info(c, "createSSLClientWithTrust", "Remote truststore successfully copied to " + localTrustStore);
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server, SSLBasicAuthClient.DEFAULT_REALM, SSLBasicAuthClient.DEFAULT_SERVLET_NAME, SSLBasicAuthClient.DEFAULT_CONTEXT_ROOT, localKeyStore
                        .getAbsolutePath(), serverKeyStorePassword, localTrustStore.getAbsolutePath(), serverTrustStorePassword, sslProtocol);
        return sslClient;
    }

    /**
     * Creates an SSLBasicAuthClient with trust from the remote server keystore.
     *
     * @return
     * @throws Exception
     */
    protected SSLBasicAuthClient createOSGISSLClientWithTrust(String serverTrustStorePath, String serverTrustStorePassword) throws Exception {
        Log.info(c, "createOSGISSLClientWithTrust", "Copying remote truststore: " + serverTrustStorePath);
        RemoteFile remoteTrustStore = server.getFileFromLibertyServerRoot(serverTrustStorePath);
        RemoteFile localTrustStore = new RemoteFile(Machine.getLocalMachine(), LOCAL_TRUSTSTORE_PATH);
        localTrustStore.copyFromSource(remoteTrustStore);
        assertTrue("Remote truststore did not copy to the local machine",
                   localTrustStore.exists());
        Log.info(c, "createOSGISSLClientWithTrust", "Remote truststore successfully copied to " + localTrustStore);
        SSLBasicAuthClient sslClient = new SSLBasicAuthClient(server, SSLBasicAuthClient.DEFAULT_REALM, SSLBasicAuthClient.DEFAULT_SERVLET_NAME, "/BasicAuthWAB", null, null, localTrustStore
                        .getAbsolutePath(), serverTrustStorePassword);
        return sslClient;
    }

    /**
     * Delete the file if it exists. If we can't delete it, then
     * throw an exception as we need to be able to delete these files.
     *
     * @param server
     * @param filePath
     * @throws Exception
     */
    protected void deleteFileIfExists(LibertyServer server, String filePath) throws Exception {
        if (fileExists(server, filePath)) {
            if (!server.getFileFromLibertyServerRoot(filePath).delete()) {
                throw new Exception("Delete action failed for file: " + filePath);
            }

            // Double check to make sure the file is gone
            if (fileExists(server, filePath))
                throw new Exception("Unable to delete file: " + filePath);
        }

    }

    /**
     * Check to see if the file exists. We will wait a bit to ensure
     * that the system was not slow to flush the file.
     *
     * @param server
     * @param filePath
     * @return
     */
    protected boolean fileExists(LibertyServer server, String filePath) {
        try {
            RemoteFile remote = server.getFileFromLibertyServerRoot(filePath);
            boolean exists = false;
            int count = 0;
            do {
                //sleep half a second
                Thread.sleep(500);
                exists = remote.exists();
                count++;
            }
            //wait up to 10 seconds for the key file to appear
            while ((!exists) && count < 20);

            return exists;

        } catch (Exception e) {
            // assume the file does not exist and move on
        }

        // if we make it here assume it does not exists
        return false;
    }

    /**
     * Check to see if the default certificate was created.
     * Verify it was created with the correct size and signature algorithm.
     *
     * @param server
     * @param filePath
     * @return
     */
    protected boolean verifyDefaultCert(LibertyServer server, String filePath) {
        try {
            Log.info(c, "verifyDefaultCert", "Verify Remote keystore successfully was created correctly");

            RemoteFile remoteKeyStore = server.getFileFromLibertyServerRoot(filePath);
            RemoteFile localKeyStore = new RemoteFile(Machine.getLocalMachine(), LOCAL_TRUSTSTORE_PATH);
            localKeyStore.copyFromSource(remoteKeyStore);
            assertTrue("Remote truststore did not copy to the local machine",
                       localKeyStore.exists());

            InputStream is = localKeyStore.openForReading();

            KeyStore keystore = null;
            if (filePath.endsWith("p12"))
                keystore = KeyStore.getInstance("PKCS12");
            else
                keystore = KeyStore.getInstance("JKS");

            keystore.load(is, "Liberty".toCharArray());
            is.close();

            X509Certificate cert = (X509Certificate) keystore.getCertificate("default");

            //Now we have a certificate let's make sure the signature algorithm is correct
            String sigAlg = cert.getSigAlgName();
            Log.info(c, "verifyDefaultCert", "Certificate signature algorithm " + sigAlg);
            return sigAlg.equals("SHA256withRSA");

        } catch (Exception e) {
            // assume the file does not exist and move on
            Log.info(c, "verifyDefaultCert", "Exception hit " + e.getMessage());
        }

        // if we make it here something is wrong with cert and
        return false;
    }

    /**
     * @param ext
     * @param subject
     * @param hostname
     * @return
     */
    protected int createTestCert(LibertyServer libertyServer, String subject, String ext) {

        ProgramOutput po = null;
        String subjectDN = "CN=nohost,O=ibm,C=us";
        String extension = "BC=ca:true";

        if (subject != null)
            subjectDN = subject;

        if (ext != null)
            extension = ext;

        try {
            po = libertyServer.getMachine().execute(System.getProperty("java.home") + "/bin/keytool",
                                                    new String[] { "-genKeyPair",
                                                                   "-keystore", server.getServerRoot() + "/resources/security/key.p12",
                                                                   "-alias", "default",
                                                                   "-storepass", "password",
                                                                   "-keypass", "password",
                                                                   "-keyalg", "RSA",
                                                                   "-sigalg", "sha256withRSA",
                                                                   "-keysize", "2048",
                                                                   "-storetype", "PKCS12",
                                                                   "-dname", subjectDN,
                                                                   "-ext", extension
                                                    },
                                                    server.getServerRoot(),
                                                    null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.info(c, name.getMethodName(), "genKeyPair RC: " + po.getReturnCode());
        Log.info(c, name.getMethodName(), "genKeyPair stdout:\n" + po.getStdout());
        Log.info(c, name.getMethodName(), "genKeyPair stderr:\n" + po.getStderr());

        return po.getReturnCode();

    }

}

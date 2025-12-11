/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package componenttest.containers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStore.Builder;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.io.Files;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;

import componenttest.containers.KeystoreBuilder.MigratingCertificate;
import componenttest.containers.KeystoreBuilder.STORE_TYPE;
import componenttest.topology.impl.LibertyServer;

public class KeystoreBuilderTest {

    @Mock
    LibertyServer server;

    @Mock
    GenericContainer<?> cont;

    private AutoCloseable mockedObjets;

    @Before
    public void init() {
        mockedObjets = MockitoAnnotations.openMocks(this);
    }

    public void commonSetup() {
        when(cont.isRunning()).thenReturn(true);
        when(server.isStarted()).thenReturn(false);
    }

    @After
    public void close() throws Exception {
        mockedObjets.close();
    }

    @Test
    public void testMigratingCertificateTempDestination() {
        commonSetup();

        //Copy file from classpath instead of from inside container because it doesn't actually exist
        doAnswer(invocation -> {
            URL src = KeystoreBuilderTest.class.getResource("test.crt");
            String dest = (String) invocation.getArgument(1); // The second parameter

            FileUtils.copyURLToFile(src, new File(dest));

            return null;
        }).when(cont).copyFileFromContainer(Mockito.anyString(), Mockito.anyString());

        MigratingCertificate mc = MigratingCertificate.of("/fake/path/in/container/server.crt");

        try {
            mc.load(); // out of order call
            fail("Should not have been able to load certifcate before migration.");
        } catch (IllegalStateException e) {
            // expected
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception:" + e.getLocalizedMessage());
        }

        Certificate cert = mc.migrate(cont).load();
        assertNotNull(cert);
    }

    @Test
    public void testMigratingCertificateDestination() {
        commonSetup();

        //Copy file from classpath instead of from inside container because it doesn't actually exist
        doAnswer(invocation -> {
            URL src = KeystoreBuilderTest.class.getResource("test.crt");
            String dest = (String) invocation.getArgument(1); // The second parameter

            FileUtils.copyURLToFile(src, new File(dest));

            return null;
        }).when(cont).copyFileFromContainer(Mockito.anyString(), Mockito.anyString());

        File testDestination = new File(System.getProperty("java.io.tmpdir"), "testMigratingCertificateDestination.cert");
        testDestination.deleteOnExit();

        MigratingCertificate mc = MigratingCertificate.of("/fake/path/in/container/cert.txt", testDestination.getAbsolutePath());

        assertFalse(testDestination.exists());

        mc = mc.migrate(cont);

        assertTrue(testDestination.exists());
        assertTrue(testDestination.isFile());

        Certificate cert = mc.load();
        assertNotNull(cert);
    }

    @Test
    public void testContainerStateFailure() {
        // Container is not running
        when(cont.isRunning()).thenReturn(false);
        when(server.isStarted()).thenReturn(false);

        try {
            KeystoreBuilder.of(server, cont);
            fail("Should not have been able to create keystore builder when container is not running");
        } catch (IllegalStateException e ) {
         // expected
        } catch (Exception e) {
         // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testServerStateFailure() {
        // Server is started
        when(cont.isRunning()).thenReturn(true);
        when(server.isStarted()).thenReturn(true);

        try {
            KeystoreBuilder.of(server, cont);
            fail("Should not have been able to create keystore builder when server has started");
        } catch (IllegalStateException e ) {
         // expected
        } catch (Exception e) {
         // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testKeystoreNoConfiguration() {
        commonSetup();

        try {
            File keystore = KeystoreBuilder.of(server, cont).export();
            fail("Should not have been able to build a keystore without a destination directory. Keystore: " + keystore.getAbsolutePath());
        } catch (NullPointerException npe) {
            // expected
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

    @Test
    public void testKeystoreMiniumConfiguration() {
        commonSetup();

        //Where the keystore will end up
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "testKeystoreWithNoCertificate");

        File keystore = null;
        try {
            keystore = KeystoreBuilder.of(server, cont)
                            .withDirectory(tempDir.getAbsolutePath())
                            .export();

            System.out.println("Exported keystore to: " + keystore.getAbsolutePath());

            assertTrue("Keystore should not have been empty", keystore.length() > 0);
            verifyNoCertificateAliases(keystore, STORE_TYPE.PKCS12.name(), "liberty");
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (keystore != null && keystore.exists()) {
                keystore.delete();
            }
        }
    }

    @Test
    public void testKeystoreMaximumConfiguration() {
        commonSetup();

        //Copy file from classpath instead of from inside container because it doesn't actually exist
        doAnswer(invocation -> {
            URL src = KeystoreBuilderTest.class.getResource("test.crt");
            String dest = (String) invocation.getArgument(1); // The second parameter

            FileUtils.copyURLToFile(src, new File(dest));

            return null;
        }).when(cont).copyFileFromContainer(Mockito.anyString(), Mockito.anyString());

        // Where the migrating certificate will end up
        File testDestination = new File(System.getProperty("java.io.tmpdir"), "testKeystoreMaximumConfiguration.cert");
        testDestination.deleteOnExit();

        // Where the keystore will end up
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "testKeystoreMaximumConfiguration");

        File keystore = null;
        try {
            keystore = KeystoreBuilder.of(server, cont)
                            .withCertificate("myAlias", "/fake/path/in/container/cert.txt", testDestination.getAbsolutePath())
                            .withDirectory(tempDir.getAbsolutePath())
                            .withFilename("testKeystoreMaximumConfiguration.jks")
                            .withStoreType(STORE_TYPE.PKCS12)
                            .withPassword("FakePassword123")
                            .export();

            System.out.println("Exported keystore to: " + keystore.getAbsolutePath());

            assertTrue("Keystore should not have been empty", keystore.length() > 0);
            assertTrue("Extension should have been replaced with p12 but was " + Files.getFileExtension(keystore.getAbsolutePath()),
                       Files.getFileExtension(keystore.getAbsolutePath()).equals("p12"));
            verifyCertificateAlias("myAlias", keystore, STORE_TYPE.PKCS12.name(), "FakePassword123");
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (keystore != null && keystore.exists()) {
                keystore.delete();
            }
        }
    }

    @Test
    public void testExistingKeystore() throws Exception {
        commonSetup();

        //Copy file from classpath instead of from inside container because it doesn't actually exist
        doAnswer(invocation -> {
            URL src = KeystoreBuilderTest.class.getResource("test.crt");
            String dest = (String) invocation.getArgument(1); // The second parameter

            FileUtils.copyURLToFile(src, new File(dest));

            return null;
        }).when(cont).copyFileFromContainer(Mockito.anyString(), Mockito.anyString());

        // Copy the existing keystore to a known location
        URL classpathKeystore = KeystoreBuilderTest.class.getResource("test.p12");
        File destinationKeystore = new File(System.getProperty("java.io.tmpdir"), "testExistingKeystore.p12");
        FileUtils.copyURLToFile(classpathKeystore, destinationKeystore);

        //Ensure keystore doesn't already have a trusted certificate with alias myAlias, but does have a user certificate
        assertTrue("Keystore file should have been copied from classpath.", destinationKeystore.exists());
        verifyNoCertificateAlias("myAlias", destinationKeystore, STORE_TYPE.PKCS12.name(), "liberty");
        verifyCertificateAlias("user", destinationKeystore, STORE_TYPE.PKCS12.name(), "liberty");

        File outputKeystore = null;
        try {
            outputKeystore = KeystoreBuilder.of(server, cont)
                            .withCertificate("myAlias", "/fake/path/in/container/cert.txt")
                            .withDirectory(destinationKeystore.getParent())
                            .withFilename(destinationKeystore.getName())
                            .withStoreType(STORE_TYPE.PKCS12)
                            .withPassword("liberty")
                            .export();

            System.out.println("Appended to keystore at: " + outputKeystore.getAbsolutePath());

            assertTrue("Keystore should not have been empty", outputKeystore.length() > 0);
            verifyCertificateAlias("myAlias", outputKeystore, STORE_TYPE.PKCS12.name(), "liberty");
            verifyCertificateAlias("user", outputKeystore, STORE_TYPE.PKCS12.name(), "liberty");
        } catch (Exception e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        } finally {
            if (destinationKeystore != null && destinationKeystore.exists()) {
                destinationKeystore.delete();
            }
        }
    }

    private void verifyCertificateAlias(String alias, File keystore, String storetype, String password) {
        Builder builder = KeyStore.Builder.newInstance(storetype, null, keystore, new PasswordProtection(password.toCharArray()));

        try {
            KeyStore loadedTruststore = builder.getKeyStore();
            assertTrue("Truststore should have had alias " + alias, loadedTruststore.containsAlias(alias));
        } catch (KeyStoreException e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

    private void verifyNoCertificateAliases(File keystore, String storetype, String password) {
        Builder builder = KeyStore.Builder.newInstance(storetype, null, keystore, new PasswordProtection(password.toCharArray()));

        try {
            KeyStore loadedTruststore = builder.getKeyStore();
            String alias = loadedTruststore.aliases().hasMoreElements() ? loadedTruststore.aliases().nextElement() : "";
            assertFalse("Truststore should not have had any aliases, but had: " + alias, loadedTruststore.aliases().hasMoreElements());
        } catch (KeyStoreException e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

    private void verifyNoCertificateAlias(String alias, File keystore, String storetype, String password) {
        Builder builder = KeyStore.Builder.newInstance(storetype, null, keystore, new PasswordProtection(password.toCharArray()));

        try {
            KeyStore loadedTruststore = builder.getKeyStore();
            assertFalse("Truststore should not have had alias " + alias, loadedTruststore.containsAlias(alias));
        } catch (KeyStoreException e) {
            // unexpected
            fail("Caught unexpected exception: " + e.getLocalizedMessage());
        }
    }

}

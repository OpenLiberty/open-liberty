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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.io.Files;

import componenttest.topology.impl.LibertyServer;

/**
 * A builder classes that will copy certificates and/or keys out of a container
 * and add them to a keystore file locally for testing.
 */
public class KeystoreBuilder {

    /** Standard store types */
    public static enum STORE_TYPE {
        JKS(".jks"),
        PKCS12(".p12");

        public String extension;

        private STORE_TYPE(String extension) {
            this.extension = extension;
        }
    };

    // Required fields for builder
    private final LibertyServer server;
    private final GenericContainer<?> container;

    // Required configurations
    private String trustoreDirectory;

    // Default configurations
    private String truststoreFilename = "truststore";
    private String truststorePassword = "liberty";
    private STORE_TYPE storeType = STORE_TYPE.PKCS12;
    private final Map<String, MigratingCertificate> certificates = new HashMap<>();

    ///// CONSTRUCTOR /////

    private KeystoreBuilder(LibertyServer server, GenericContainer<?> container) {
        this.server = server;
        this.container = container;
    }

    ///// BUILDER /////

    public static KeystoreBuilder of(LibertyServer server, GenericContainer<?> container) {
        validateState(server, container);
        return new KeystoreBuilder(server, container);
    }

    ///// CONFIGURATIONS /////

    /**
     * A certificate that exists in the container.
     * This can be called multiple times to add more certificates.
     *
     * @param  alias  the certificate alias
     * @param  source the fully qualified path to the certificate in the container
     *
     * @return        this
     */
    public KeystoreBuilder withCertificate(String alias, String source) {
        certificates.put(alias, MigratingCertificate.of(source));
        return this;
    }

    /**
     * A certificate that exists in the container.
     * This can be called multiple times to add more certificates.
     *
     * @param  alias       the certificate alias
     * @param  source      the fully qualified path to the certificate in the container
     * @param  destination the fully qualified path to a client location where the certificate will be copied
     *
     * @return             this
     */
    public KeystoreBuilder withCertificate(String alias, String source, String destination) {
        certificates.put(alias, MigratingCertificate.of(source, destination));
        return this;
    }

    /**
     * The directory into which the keystore will be exported.
     *
     * @param  directory fully qualified directory path
     *
     * @return           this
     */
    public KeystoreBuilder withDirectory(String directory) {
        Objects.requireNonNull(directory);
        this.trustoreDirectory = directory;
        return this;
    }

    /**
     * The file name used to export the keystore with or without extension.
     *
     * @param  filename the keystore filename
     *
     * @return
     */
    public KeystoreBuilder withFilename(String filename) {
        Objects.requireNonNull(filename);
        this.truststoreFilename = Files.getNameWithoutExtension(filename);
        return this;
    }

    public KeystoreBuilder withStoreType(STORE_TYPE type) {
        this.storeType = type;
        return this;
    }

    public KeystoreBuilder withPassword(String password) {
        Objects.requireNonNull(password);
        this.truststorePassword = password;
        return this;
    }

    ///// TERMINATOR /////
    public File export() {
        validateState(server, container);

        Objects.requireNonNull(trustoreDirectory, "Must configure a truststore directory before calling export()");

        File truststoreFile = new File(trustoreDirectory, truststoreFilename + storeType.extension);

        KeyStore ks;
        if (truststoreFile.exists()) {
            // Load existing KeyStore object
            try (FileInputStream fis = new FileInputStream(truststoreFile)) {
                ks = KeyStore.getInstance(storeType.name());
                ks.load(fis, truststorePassword.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException("Could not create in-memory keystore from existing keystore: " + truststoreFile.getAbsolutePath(), e);
            }
        } else {
            // Create file for later storage
            try {
                Files.createParentDirs(truststoreFile);
                Files.touch(truststoreFile);
            } catch (Exception e) {
                throw new RuntimeException("Unable to create new keystore file at: " + truststoreFile.getAbsolutePath(), e);
            }
            // Create a new KeyStore object
            try {
                ks = KeyStore.getInstance(storeType.name());
                ks.load(null, truststorePassword.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException("Could not create in-memory keystore for storage type: " + storeType.name(), e);
            }
        }

        // Insert the certificate(s) using an alias
        certificates.forEach((alias, cert) -> {
            try {
                ks.setCertificateEntry(alias, cert.migrate(container).load());
            } catch (KeyStoreException e) {
                throw new RuntimeException("Could not add certificate to in-memory keystore. Alias: " + alias, e);
            }
        });

        // Persist keystore to disk
        try (FileOutputStream fos = new FileOutputStream(truststoreFile.getAbsolutePath())) {
            ks.store(fos, truststorePassword.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException("Could not persist keystore to disk", e);
        }

        return truststoreFile;
    }

    ///// VERIFIER /////

    private static void validateState(LibertyServer server, GenericContainer<?> container) {
        Objects.requireNonNull(server);
        Objects.requireNonNull(container);

        if (server.isStarted()) {
            throw new IllegalStateException("Cannot build a keystore file after the server has started.");
        }

        if (!container.isRunning()) {
            throw new IllegalStateException("Cannot build a keystore file before the container is running.");
        }
    }

    /**
     * Represents a certificate that will migrate from the container
     */
    static class MigratingCertificate {

        /**
         * Tracks the state of certificate migration.
         * CONFIGURED -> MIGRATED -> LOADED
         */
        private static enum STATE {
            CONFIGURED,
            MIGRATED,
            LOADED
        }

        /** The only supported format for the underlying CertificateFactory class */
        private static final String TYPE = "X.509";

        /** Fully qualified path to the certificate in the container */
        private final String srcPath;

        /** Fully qualified path to the certificate on host system */
        private final String destPath;

        /** Keep track of state */
        private STATE state;

        // Private constructor
        private MigratingCertificate(String srcPath, String destPath) {
            this.srcPath = srcPath;
            this.destPath = destPath;
            this.state = STATE.CONFIGURED;
        }

        /**
         * Create instance of MigratingCertificate with both a source and destination
         *
         * @param  srcPath  The path to the certificate in the container.
         * @param  destPath The destination of the certificate on the host system.
         *
         * @return          instance
         */
        public static MigratingCertificate of(String srcPath, String destPath) {
            Objects.requireNonNull(srcPath);
            Objects.requireNonNull(destPath);

            return new MigratingCertificate(srcPath, destPath);
        }

        /**
         * Create instance of MigratingCertificate with a source.
         * The certificate will migrate to a temporary location on the host system.
         *
         * @param  srcPath The path to the certificate in the container.
         *
         * @return         instance
         */
        public static MigratingCertificate of(String srcPath) {
            Objects.requireNonNull(srcPath);

            String filename = Files.getNameWithoutExtension(srcPath);
            String extension = Files.getFileExtension(srcPath);

            File tempFile;
            try {
                tempFile = File.createTempFile(filename, extension);
            } catch (Exception e) {
                throw new RuntimeException("Could not create a temporary file", e);
            }
            tempFile.delete();

            return new MigratingCertificate(srcPath, tempFile.getAbsolutePath());
        }

        /**
         * Migrates certificate from container onto host system.
         *
         * @param  container the container that holds the certificate
         *
         * @return           this
         */
        public MigratingCertificate migrate(GenericContainer<?> container) {
            if (this.state != STATE.CONFIGURED) {
                throw new IllegalStateException("Attempted to copy certifcate outside of CONFIGURED state.  State: " + this.state);
            }

            container.copyFileFromContainer(srcPath, destPath);
            this.state = STATE.MIGRATED;
            return this;
        }

        /**
         * Loads the certificate from the host system into memory.
         *
         * @return                  The certificate
         *
         * @throws RuntimeException If any IO or Certificate exceptions occur during the loading process
         */
        public Certificate load() {
            if (this.state != STATE.MIGRATED) {
                throw new IllegalStateException("Attempted to load certificate outside of the MIGRATED state. State: " + this.state);
            }

            Certificate cert;
            CertificateFactory cf;
            try {
                cf = CertificateFactory.getInstance(TYPE);
            } catch (CertificateException e) {
                throw new RuntimeException("Could create a certificate factory for the type: " + TYPE, e);
            }

            try (FileInputStream fis = new FileInputStream(destPath)) {
                cert = cf.generateCertificate(fis);
            } catch (IOException e) {
                throw new RuntimeException("Could not read certificate file at location: " + destPath, e);
            } catch (CertificateException e) {
                throw new RuntimeException("Could not load certificate into memeory: " + destPath, e);
            }

            this.state = STATE.LOADED;
            return cert;
        }
    }

}

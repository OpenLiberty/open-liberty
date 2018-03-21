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
package com.ibm.ws.install.internal;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import org.apache.aries.util.manifest.ManifestProcessor;

import com.ibm.ws.repository.common.enums.LicenseType;

import wlp.lib.extract.LicenseProvider;
import wlp.lib.extract.ZipLicenseProvider;

/**
 * This class provides APIs to get Archive information.
 */
public final class ArchiveUtils {
    private ArchiveUtils() {}

    /* MANIFEST main attribute names */
    public static final String APPLIES_TO = "Applies-To";
    public static final String ARCHIVE_CONTENT_TYPE = "Archive-Content-Type";
    public static final String ARCHIVE_ROOT = "Archive-Root";
    public static final String REQUIRED_FEATURES = "Require-Feature";
    public static final String LICENSE_AGREEMENT = "License-Agreement";
    public static final String LICENSE_INFORMATION = "License-Information";

    /**
     * Enum for Archive File Types
     */
    public static enum ArchiveFileType {
        JAR(".jar"),
        ZIP(".zip"),
        PAX(".pax"),
        ESA(".esa");

        private final String extension;

        private ArchiveFileType(String extension) {
            this.extension = extension;
        }

        public String getFileExtension() {
            return this.extension;
        }

        public boolean isType(String archiveFile) {
            return (null == archiveFile || archiveFile.equals("")) ? false : archiveFile.toLowerCase().endsWith(this.extension);
        }
    }

    /**
     * Enum for Archive Content Type
     */
    public static enum ArchiveContentType {
        ADDON("addon", false),
        INSTALL("install", true),
        SAMPLE("sample", true),
        OPENSOURCE("osi", false);

        private final String name;
        private final boolean serverPackage;

        private ArchiveContentType(String name, boolean serverPackage) {
            this.name = name;
            this.serverPackage = serverPackage;
        }

        public boolean isServerPackage() {
            return this.serverPackage;
        }

        public boolean isContentType(String name) {
            return this.name.equalsIgnoreCase(name);
        }

        public String getName() {
            return name;
        }
    }

    /**
     * Gets the archive file Manifest attributes as a String Map from a jar file.
     *
     * @param jarFile the Jar File to be processes
     * @return a Map of attributes and values compiled from the jar file's manifest file
     * @throws Throwable
     */
    public static Map<String, String> processArchiveManifest(final JarFile jarFile) throws Throwable {
        Map<String, String> manifestAttrs = null;

        if (jarFile != null) {
            try {
                manifestAttrs = AccessController.doPrivileged(new PrivilegedExceptionAction<Map<String, String>>() {
                    @Override
                    public Map<String, String> run() throws ZipException, IOException {
                        InputStream is = null;
                        Map<String, String> attrs = null;
                        try {
                            // Read the manifest and access the manifest headers.
                            is = jarFile.getInputStream(jarFile.getEntry(JarFile.MANIFEST_NAME));
                            attrs = ManifestProcessor.readManifestIntoMap(ManifestProcessor.parseManifest(is));
                        } finally {
                            if (is != null) {
                                InstallUtils.close(is);
                            }
                        }

                        return attrs;
                    }
                });
            } catch (PrivilegedActionException e) {
                throw e.getCause();
            }
        }

        return manifestAttrs;
    }

    /**
     * Gets the archive file Manifest attributes as a String Map from a file.
     *
     * @param file the file to be processed
     * @return a Map of attributes and values compiled from the file's manifest file
     * @throws Throwable
     */
    public static Map<String, String> processArchiveManifest(final File file) throws Throwable {
        Map<String, String> manifestAttrs = null;

        if (file != null && file.isFile() && ArchiveFileType.JAR.isType(file.getPath())) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(file);
                manifestAttrs = processArchiveManifest(jarFile);
            } finally {
                if (jarFile != null) {
                    InstallUtils.close(jarFile);
                }
            }
        }

        return manifestAttrs;
    }

    /**
     * Gets the license agreement for the jar file.
     *
     * @param jar the jar file
     * @param manifestAttrs the manifest file for the jar file
     * @return the license agreement as a String
     */
    public static String getLicenseAgreement(final JarFile jar, final Map<String, String> manifestAttrs) {
        String licenseAgreement = null;

        if (manifestAttrs.isEmpty()) {
            return licenseAgreement;
        }

        String licenseAgreementPrefix = manifestAttrs.get(ArchiveUtils.LICENSE_AGREEMENT);

        LicenseProvider licenseProvider = (licenseAgreementPrefix != null) ? ZipLicenseProvider.createInstance(jar, licenseAgreementPrefix) : null;

        if (licenseProvider != null)
            licenseAgreement = new InstallLicenseImpl("", LicenseType.UNSPECIFIED, licenseProvider).getAgreement();

        return licenseAgreement;
    }
}

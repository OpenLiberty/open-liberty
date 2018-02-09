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
package com.ibm.ws.install.internal.asset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.ArchiveUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.kernel.boot.cmdline.Utils;

public class ServerPackageJarAsset extends JarAsset implements ServerPackageAsset {
    private final Collection<ServerAsset> servers = new HashSet<ServerAsset>();
    private final Collection<String> requiredFeatures = new HashSet<String>();

    private Map<String, String> manifestAttrs = new HashMap<String, String>(0);

    public ServerPackageJarAsset(File assetFile, boolean temporary) throws InstallException {
        super(assetFile, temporary);
        setManifestAttributes();
        validation();
        processServers();
        processRequiredFeaturesFromManifest();
    }

    @Override
    public boolean isServerPackage() {
        return true;
    }

    public static boolean validType(String filename) {
        return filename != null && filename.toLowerCase().endsWith(JAR_EXT);
    }

    @Override
    public Collection<ServerAsset> getServers() {
        return Collections.unmodifiableCollection(servers);
    }

    @Override
    public Collection<String> getRequiredFeatures() {
        return Collections.unmodifiableCollection(requiredFeatures);
    }

    @Override
    public Enumeration<? extends ZipEntry> getPackageEntries() {
        return jar.entries();
    }

    @Override
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return jar.getInputStream(entry);
    }

    private void setManifestAttributes() throws InstallException {
        try {
            manifestAttrs = ArchiveUtils.processArchiveManifest(jar);
        } catch (Throwable e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE",
                                                                                      getAsset().getAbsolutePath()), e, InstallException.IO_FAILURE);
        }
    }

    private void validation() throws InstallException {
        ArchiveUtils.ArchiveContentType archiveContentType = getArchiveContentType(manifestAttrs);

        if (null == archiveContentType || !archiveContentType.isServerPackage()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE", getAsset().getAbsolutePath()));
        } else if (ArchiveUtils.ArchiveContentType.INSTALL.equals(archiveContentType)) {
            String appliesTo = manifestAttrs.get(ArchiveUtils.APPLIES_TO);

            if (null == appliesTo || appliesTo.isEmpty()) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_SERVER_PACKAGE_CONTAINS_RUNTIME", getAsset().getAbsolutePath()));
            }
        }
    }

    private String getArchiveRoot() {
        String archRoot = manifestAttrs.get(ArchiveUtils.ARCHIVE_ROOT);

        return (null != archRoot) ? archRoot : "";
    }

    private void processServers() throws InstallException {
        String rootDir = getArchiveRoot();

        // We only want to the top most directory of the archive root.
        if (!rootDir.isEmpty()) {
            if (!rootDir.contains("/")) {
                rootDir += "/";
            } else if (rootDir.indexOf("/") != rootDir.lastIndexOf("/") || !rootDir.endsWith("/")) {
                String[] dirs = rootDir.split("/");

                if (dirs.length > 0) {
                    rootDir = (rootDir.startsWith("/")) ? "/" + dirs[1] + "/" : dirs[0] + "/";
                }
            }
        }

        for (Enumeration<JarEntry> jarEntries = jar.entries(); jarEntries.hasMoreElements();) {
            JarEntry nextEntry = jarEntries.nextElement();
            String entryName = nextEntry.getName().toLowerCase();
            if (!nextEntry.isDirectory()) {
                if (entryName.contains("/" + SERVER_DIR_NAME + "/") && entryName.endsWith("/" + SERVER_FILENAME)) {
                    // Get the path to where the server.xml is (or will be) in the Liberty install
                    File serverXML = new File(Utils.getInstallDir(), nextEntry.getName().substring(rootDir.length()));
                    String serverName = serverXML.getParentFile().getName();

                    servers.add(new ServerAsset(serverName, serverXML));
                }
            }
        }

        if (servers.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE", getAsset().getAbsolutePath()));
        }
    }

    private void processRequiredFeaturesFromManifest() {
        if (manifestAttrs.isEmpty()) {
            return;
        }

        String reqFeatures = manifestAttrs.get(ArchiveUtils.REQUIRED_FEATURES);

        if (null != reqFeatures && !reqFeatures.isEmpty()) {
            requiredFeatures.addAll(Arrays.asList(reqFeatures.split(",")));
        }

    }
}

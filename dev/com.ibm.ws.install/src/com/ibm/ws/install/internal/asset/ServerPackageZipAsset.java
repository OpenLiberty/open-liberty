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
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;

public class ServerPackageZipAsset extends InstallAsset implements ServerPackageAsset {
    public static final String ZIP_EXT = ".zip";
    public static final String PAX_EXT = ".pax";

    private final Collection<ServerAsset> servers = new HashSet<ServerAsset>();
    private final Collection<String> requiredFeatures = new HashSet<String>();
    private final ZipFile zip;

    public ServerPackageZipAsset(File assetFile, boolean temporary) throws InstallException {
        super(assetFile, temporary);
        try {
            this.zip = new ZipFile(assetFile);
        } catch (ZipException ze) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE",
                                                                                      getAsset().getAbsoluteFile()), ze, InstallException.IO_FAILURE);
        } catch (IOException ioe) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE",
                                                                                      getAsset().getAbsoluteFile()), ioe, InstallException.IO_FAILURE);
        }
        processServers();
    }

    @Override
    public boolean isServerPackage() {
        return true;
    }

    public static boolean validType(String filename) {
        return filename != null && (filename.toLowerCase().endsWith(ZIP_EXT) || filename.toLowerCase().endsWith(PAX_EXT));
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
        return zip.entries();
    }

    @Override
    public InputStream getInputStream(ZipEntry entry) throws IOException {
        return zip.getInputStream(entry);
    }

    @Override
    public void delete() {
        InstallUtils.close(zip);
        super.delete();
    }

    private void processServers() throws InstallException {
        for (Enumeration<? extends ZipEntry> zipEntries = zip.entries(); zipEntries.hasMoreElements();) {
            ZipEntry nextEntry = zipEntries.nextElement();
            String entryName = nextEntry.getName().toLowerCase();
            if (nextEntry.isDirectory()) {
                if (entryName.startsWith("wlp/bin/")) {
                    throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_SERVER_PACKAGE_CONTAINS_RUNTIME",
                                                                                              getAsset().getAbsolutePath()));
                }
            } else {
                if (entryName.contains("/servers/") && entryName.endsWith("/server.xml")) {
                    String dirs[] = nextEntry.getName().split("/");
                    String serverName = dirs[dirs.length - 2];
                    File serverDir = new File(InstallUtils.getServersDir(), serverName);
                    File serverXML = new File(serverDir, InstallUtils.SERVER_XML);
                    servers.add(new ServerAsset(serverName, serverXML));
                }
            }
        }

        if (servers.isEmpty()) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_INVALID_SERVER_PACKAGE", getAsset().getAbsolutePath()));
        }
    }
}

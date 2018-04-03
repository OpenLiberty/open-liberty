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
import java.util.Map;
import java.util.jar.JarFile;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.ArchiveUtils;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.repository.resources.RepositoryResource;
import com.ibm.ws.repository.resources.SampleResource;

/**
 *
 */
public class JarAsset extends InstallAsset {

    public static final String JAR_EXT = ".jar";

    protected JarFile jar = null;
    protected String shortName = null;
    private SampleResource sampleResource = null;

    public JarAsset(File assetFile, boolean temporary) throws InstallException {
        super(assetFile, temporary);
        try {
            jar = new JarFile(assetFile);
        } catch (IOException e) {
            throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_READ_FILE", getAsset().getAbsoluteFile(),
                                                                                      e.getMessage()), e, InstallException.RUNTIME_EXCEPTION);
        }
    }

    public JarAsset(String id, File assetFile, boolean isTemporary) throws InstallException {
        super(id, assetFile, isTemporary);

        if (assetFile != null) {
            try {
                jar = new JarFile(assetFile);
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_READ_FILE", getAsset().getAbsoluteFile(),
                                                                                          e.getMessage()), e, InstallException.RUNTIME_EXCEPTION);
            }
        }
    }

    public JarAsset(String id, String shortName, File assetFile, boolean isTemporary) throws InstallException {
        this(id, assetFile, isTemporary);
        this.shortName = shortName;
    }

    public JarAsset(SampleResource sampleResource) throws InstallException {
        this(sampleResource.getName(), null, true);
        this.shortName = sampleResource.getShortName();
        this.sampleResource = sampleResource;
    }

    @Override
    public void delete() {
        InstallUtils.close(jar);
        super.delete();
    }

    public JarFile getJar() {
        return this.jar;
    }

    public String getJarPath() {
        return getAsset().getAbsolutePath();
    }

    public String getShortName() {
        return shortName;
    }

    protected ArchiveUtils.ArchiveContentType getArchiveContentType(Map<String, String> manifestAttrs) {
        String archiveContentType = manifestAttrs.get(ArchiveUtils.ARCHIVE_CONTENT_TYPE);

        if (ArchiveUtils.ArchiveContentType.INSTALL.isContentType(archiveContentType)) {
            return ArchiveUtils.ArchiveContentType.INSTALL;
        } else if (ArchiveUtils.ArchiveContentType.SAMPLE.isContentType(archiveContentType)) {
            return ArchiveUtils.ArchiveContentType.SAMPLE;
        } else if (ArchiveUtils.ArchiveContentType.ADDON.isContentType(archiveContentType)) {
            return ArchiveUtils.ArchiveContentType.ADDON;
        } else if (ArchiveUtils.ArchiveContentType.OPENSOURCE.isContentType(archiveContentType)) {
            return ArchiveUtils.ArchiveContentType.OPENSOURCE;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return shortName == null ? this.name : shortName;
    }

    @Override
    public RepositoryResource getRepositoryResource() {
        return this.sampleResource;
    }

    @Override
    public void download(File installTempDir) throws InstallException {
        if (sampleResource == null)
            return;
        asset = download(installTempDir, sampleResource);
        if (asset != null) {
            try {
                jar = new JarFile(asset);
            } catch (IOException e) {
                throw new InstallException(Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("ERROR_UNABLE_READ_FILE", getAsset().getAbsoluteFile(),
                                                                                          e.getMessage()), e, InstallException.RUNTIME_EXCEPTION);
            }
        }
    }

    @Override
    public void cleanup() {
        if (sampleResource == null)
            return;
        delete();
        jar = null;
        asset = null;
    }
}

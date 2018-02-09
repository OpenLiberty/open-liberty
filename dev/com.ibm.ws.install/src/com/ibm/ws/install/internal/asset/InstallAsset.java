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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.ExceptionUtils;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.repository.resources.RepositoryResource;

public abstract class InstallAsset {
    protected static final Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);

    private static final String DEFAULT_NAME = "";
    private static final boolean DEFAULT_TEMP = true;

    protected String name;
    protected File asset;
    private final boolean temporary;

    public InstallAsset(boolean temporary) {
        this(DEFAULT_NAME, null, temporary);
    }

    public InstallAsset(File assetFile) {
        this(DEFAULT_NAME, assetFile, DEFAULT_TEMP);
    }

    public InstallAsset(File assetFile, boolean temporary) {
        this(DEFAULT_NAME, assetFile, temporary);
    }

    public InstallAsset(String name, File assetFile) {
        this(name, assetFile, DEFAULT_TEMP);
    }

    public InstallAsset(String name, File assetFile, boolean temporary) {
        this.name = name;
        this.temporary = temporary;
        this.asset = assetFile;
    }

    public void delete() {
        if (asset != null && isTemporary() && !this.asset.delete())
            this.asset.deleteOnExit();
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean isFeature() {
        return false;
    }

    public boolean isFix() {
        return false;
    }

    public boolean isServerPackage() {
        return false;
    }

    protected boolean isTemporary() {
        return temporary;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isAddon() {
        return false;
    }

    public boolean isSample() {
        return false;
    }

    public boolean isOpenSource() {
        return false;
    }

    public File getAsset() {
        return asset;
    }

    public String installedLogMsg() {
        return "";
    }

    public static void log(String msg) {
        if (logger != null)
            logger.log(Level.FINE, msg);
    }

    protected File download(File installTempDir, RepositoryResource installResource) throws InstallException {
        try {
            String url = installResource.getMainAttachment().getURL();
            logger.log(Level.FINEST,
                       "Downloading "
                                     + installResource.getName()
                                     + " from "
                                     + (url.contains("public.dhe.ibm.com") ? "the IBM WebSphere Liberty Repository" : url
                                                                                                                      + " of the repository "
                                                                                                                      + installResource.getRepositoryConnection().getRepositoryLocation()));

            File d = InstallUtils.download(installTempDir, installResource);
            logger.log(Level.FINEST,
                       d == null ? installResource.getName() + " is an unsupported type " + installResource.getType()
                                   + " to be downloaded." : "Downloaded " + installResource.getName() + " from "
                                                            + (url.contains("public.dhe.ibm.com") ? "the IBM WebSphere Liberty Repository" : url + " of the repository "
                                                                                                                                             + installResource.getRepositoryConnection().getRepositoryLocation())
                                                            + " to " + d.getAbsolutePath());
            return d;
        } catch (RuntimeException e) {
            throw e;
        } catch (InstallException e) {
            throw e;
        } catch (Exception e) {
            throw ExceptionUtils.createFailedToDownload(installResource, e, installTempDir);
        }
    }

    public void download(File installTempDir) throws InstallException {}

    public RepositoryResource getRepositoryResource() {
        return null;
    }

    public void cleanup() {}
}

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
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.repository.resources.IfixResource;
import com.ibm.ws.repository.resources.RepositoryResource;

public class FixAsset extends InstallAsset {

    private JarFile jar = null;
    private Attributes mainAttributes = null;
    private String jarPath = null;
    private IfixResource ifixResource = null;

    public FixAsset(String id, File assetFile, boolean isTemporary) throws ZipException, IOException {
        super(id, assetFile, isTemporary);
        this.jar = new JarFile(assetFile);
        this.mainAttributes = jar.getManifest().getMainAttributes();
        this.jarPath = assetFile.getAbsolutePath();
    }

    public FixAsset(String id, IfixResource ifixResource) {
        super(id, null);
        this.ifixResource = ifixResource;
    }

    @Override
    public boolean isFix() {
        return true;
    }

    public JarFile getJar() {
        return this.jar;
    }

    public Attributes getMainAttributes() {
        return this.mainAttributes;
    }

    @Override
    public void delete() {
        if (jar != null) {
            try {
                jar.close();
            } catch (IOException e) {
            }
        }
        super.delete();
    }

    @Override
    public String installedLogMsg() {
        return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_FIX", toString());
    }

    public String getJarPath() {
        return jarPath;
    }

    @Override
    public RepositoryResource getRepositoryResource() {
        return this.ifixResource;
    }

    @Override
    public void download(File installTempDir) throws InstallException {
        if (ifixResource == null)
            return;
        asset = download(installTempDir, ifixResource);
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
        if (ifixResource == null)
            return;
        delete();
        jar = null;
        asset = null;
    }
}

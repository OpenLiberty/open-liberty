/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2013, 2017
 *
 * The source code for this program is not published or otherwise divested
 * of its trade secrets, irrespective of what has been deposited with the
 * U.S. Copyright Office.
 */
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

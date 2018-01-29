/*
 * IBM Confidential
 *
 * OCO Source Materials
 *
 * Copyright IBM Corp. 2015, 2016
 *
 * The source code for this program is not published or otherwise divested 
 * of its trade secrets, irrespective of what has been deposited with the 
 * U.S. Copyright Office.
 */
package com.ibm.ws.install.internal.asset;

import java.io.File;

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.repository.resources.SampleResource;

public class SampleAsset extends JarAsset {

    public SampleAsset(String id, String shortName, File assetFile, boolean isTemporary) throws InstallException {
        super(id, shortName, assetFile, isTemporary);
    }

    public SampleAsset(SampleResource sampleResource) throws InstallException {
        super(sampleResource);
    }

    @Override
    public boolean isSample() {
        return true;
    }

    @Override
    public String installedLogMsg() {
        return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_SAMPLE", toString());
    }
}

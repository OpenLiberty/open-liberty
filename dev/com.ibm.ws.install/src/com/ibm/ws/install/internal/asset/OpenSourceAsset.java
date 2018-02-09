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

import com.ibm.ws.install.InstallException;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.repository.resources.SampleResource;

public class OpenSourceAsset extends JarAsset {

    public OpenSourceAsset(String id, String shortName, File assetFile, boolean isTemporary) throws InstallException {
        super(id, shortName, assetFile, isTemporary);
    }

    public OpenSourceAsset(SampleResource sampleResource) throws InstallException {
        super(sampleResource);
    }

    @Override
    public boolean isOpenSource() {
        return true;
    }

    @Override
    public String installedLogMsg() {
        return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_INSTALLED_OPENSOURCE", toString());
    }
}

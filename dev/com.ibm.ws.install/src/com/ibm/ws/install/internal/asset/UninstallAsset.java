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

import com.ibm.ws.install.internal.InstallLogUtils.Messages;
import com.ibm.ws.install.internal.InstallUtils;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;
import com.ibm.ws.product.utility.extension.ifix.xml.IFixInfo;

/**
 *
 */
public class UninstallAsset {

    IFixInfo fixInfo;
    String name;
    String displayName;
    ProvisioningFeatureDefinition pd;
    UninstallAssetType type;

    public UninstallAsset(IFixInfo fixInfo) {
        this.fixInfo = fixInfo;
        this.name = fixInfo.getId();
        this.pd = null;
        this.type = UninstallAssetType.fix;
    }

    public UninstallAsset(ProvisioningFeatureDefinition pd) {
        String ext = pd.getBundleRepositoryType();
        String shortName = InstallUtils.getShortName(pd);
        this.name = pd.getSymbolicName();
        this.pd = pd;
        this.displayName = ext == null || ext.isEmpty() ? shortName : ext + ":" + shortName;
        this.type = UninstallAssetType.feature;
    }

    public IFixInfo getIFixInfo() {
        return fixInfo;
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return name;
    }

    public UninstallAssetType getType() {
        return type;
    }

    public ProvisioningFeatureDefinition getProvisioningFeatureDefinition() {
        return pd;
    }

    public enum UninstallAssetType {
        feature, fix
    }

    public String uninstalledLogMsg() {
        if (type == UninstallAssetType.feature) {
            return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_UNINSTALLED_FEATURE", toString()).replaceAll("CWWKF1306I:", "").trim();
        }
        if (type == UninstallAssetType.fix) {
            return Messages.INSTALL_KERNEL_MESSAGES.getLogMessage("LOG_UNINSTALLED_FIX", toString()).replaceAll("CWWKF1307I:", "").trim();
        }
        return "";
    }

}

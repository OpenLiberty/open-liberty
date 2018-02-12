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

import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.ibm.ws.install.InstallConstants;
import com.ibm.ws.install.internal.InstallLogUtils.Messages;

/**
 *
 */
public class SubsytemEntry {

    private final ZipEntry entry;

    public SubsytemEntry(ZipFile esaFile) throws IOException {
        entry = findSubsystemEntry(esaFile);
    }

    public SubsytemEntry(ZipFile indexFile, String featureName) throws IOException {
        entry = findSubsystemEntry(indexFile, featureName);
    }

    private ZipEntry findSubsystemEntry(ZipFile zip) {
        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
        ZipEntry subsystemEntry = null;
        while (zipEntries.hasMoreElements()) {
            ZipEntry nextEntry = zipEntries.nextElement();
            if ("OSGI-INF/Subsystem.mf".equalsIgnoreCase(nextEntry.getName())) {
                if (subsystemEntry != null) {
                    Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
                    logger.log(Level.WARNING, Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.two.subsystem.manifests", subsystemEntry.getName()));
                    break;
                }
                subsystemEntry = nextEntry;
            }
        }
        return subsystemEntry;
    }

    private ZipEntry findSubsystemEntry(ZipFile zip, String featureName) {
        Enumeration<? extends ZipEntry> zipEntries = zip.entries();
        ZipEntry subsystemEntry = null;
        while (zipEntries.hasMoreElements()) {
            ZipEntry nextEntry = zipEntries.nextElement();
            if (String.format("%sOSGI-INF/Subsystem.mf", featureName).equalsIgnoreCase(nextEntry.getName())) {
                if (subsystemEntry != null) {
                    Logger logger = Logger.getLogger(InstallConstants.LOGGER_NAME);
                    logger.log(Level.WARNING, Messages.PROVISIONER_MESSAGES.getLogMessage("tool.install.two.subsystem.manifests", subsystemEntry.getName()));
                    break;
                }
                subsystemEntry = nextEntry;
            }
        }
        return subsystemEntry;
    }

    public ZipEntry getSubsystemEntry() {
        return entry;
    }

    public String getSubsystemEntryName() {
        return entry.getName();
    }

}

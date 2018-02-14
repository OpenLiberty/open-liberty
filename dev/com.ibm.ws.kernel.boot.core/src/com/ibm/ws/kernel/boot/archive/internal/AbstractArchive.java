/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive.internal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.ibm.ws.kernel.boot.archive.Archive;
import com.ibm.ws.kernel.boot.archive.ArchiveEntryConfig;

public abstract class AbstractArchive implements Archive {

    protected File archiveFile;

    protected final List<ArchiveEntryConfig> configList = new ArrayList<ArchiveEntryConfig>();

    // the paths that has been added to the archive
    protected final Set<String> entryPaths = new TreeSet<String>();

    @Override
    public void addEntryConfig(ArchiveEntryConfig entryConfig) {
        configList.add(entryConfig);
    }

    @Override
    public void addEntryConfigs(List<ArchiveEntryConfig> entryConfigs) {
        configList.addAll(entryConfigs);
    }

    @Override
    public File create() throws IOException {
        Iterator<ArchiveEntryConfig> configIter = configList.iterator();

        while (configIter.hasNext()) {
            ArchiveEntryConfig config = configIter.next();
            config.configure(this);
        }

        return archiveFile;
    }

}

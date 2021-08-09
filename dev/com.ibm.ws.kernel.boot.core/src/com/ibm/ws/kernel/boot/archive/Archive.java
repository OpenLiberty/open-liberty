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
package com.ibm.ws.kernel.boot.archive;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface that represents an archive format for dump, package,
 * and whatever else comes along.
 */
public interface Archive extends Closeable {
    int DEFAULT_BUFFER_SIZE = 4096;

    /**
     * Add the archive entry config to the archive.
     * 
     * @param entryConfig
     */
    void addEntryConfig(ArchiveEntryConfig entryConfig);

    /**
     * Add a group of archive entry configs to the archive.
     * 
     * @param entryConfigList
     */
    void addEntryConfigs(List<ArchiveEntryConfig> entryConfigList);

    /**
     * Assemble the archive according to the entry path, file source of the entry configs.
     * If the archive already has an entry with the same entry path, will skip the new one, keep the old one.
     * 
     * @return
     * @throws IOException if the archive file could not be found.
     */
    File create() throws IOException;

    /**
     * Add a file from the source to the archive.
     * 
     * @param entryPath the relative target path in the archive, could be a file path or a folder path if it is end of "/".
     *            If it is a folder, we will use the same name of the source file.
     * @param source the source file. It can not be a folder.
     * @throws IOException
     */
    void addFileEntry(String entryPath, File source) throws IOException;

    /**
     * Add files in the source folder to the archive.
     * 
     * @param entryPath the relative target path in the archive.
     * @param source the source folder. It can not be a file.
     * @param dirContent the file list need add to the archive from the source folder.
     * @throws IOException
     */
    void addDirEntry(String entryPath, File source, List<String> dirContent) throws IOException;

}

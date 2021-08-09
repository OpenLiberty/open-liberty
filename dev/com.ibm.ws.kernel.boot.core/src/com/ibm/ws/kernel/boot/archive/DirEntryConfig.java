/*******************************************************************************
 * Copyright (c) 2012-2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.ibm.ws.kernel.boot.archive.DirPattern.PatternStrategy;
import com.ibm.ws.kernel.boot.internal.FileUtils;

public class DirEntryConfig implements ArchiveEntryConfig {

    protected final String entryPath;
    protected final File source;
    protected final DirPattern dirPattern;

    /**
     * Create a dir entry config.
     *
     * @param entryPath        the entry prefix added to the archive
     * @param source           the base directory
     * @param includeByDefault if a file underneath base directory is included by default when no pattern apply to it.
     * @param strategy         when a file matches both the includePattern and excludePattern, decide which take preference.
     */
    public DirEntryConfig(String entryPath, File source, boolean includeByDefault, PatternStrategy strategy) throws IOException {

        entryPath = FileUtils.normalizeEntryPath(entryPath);
        this.entryPath = FileUtils.normalizeDirPath(entryPath);

        if (!source.exists()) {
            throw new FileNotFoundException(source.getAbsolutePath());
        }
        if (!source.isDirectory()) {

            throw new IllegalArgumentException("The source is not a directory.");
        }
        this.source = source;

        this.dirPattern = new DirPattern(includeByDefault, strategy);
    }

    @Override
    public String getEntryPath() {
        return this.entryPath;
    }

    @Override
    public File getSource() {
        return this.source;
    }

    public void include(Pattern pattern) {
        dirPattern.getIncludePatterns().add(pattern);
    }

    public void exclude(Pattern pattern) {
        dirPattern.getExcludePatterns().add(pattern);
    }

    @Override
    public void configure(Archive archive) throws IOException {
        List<String> dirContent = new ArrayList<String>();
        filterDirectory(dirContent, dirPattern, "");

        archive.addDirEntry(entryPath, source, dirContent);
    }

    /**
     * filter the directory according to the dir pattern
     *
     * @param dirContent the relative paths of the files which we need add to the archive
     * @param dirPattern
     * @param parentPath
     * @throws IOException
     */
    protected void filterDirectory(List<String> dirContent, DirPattern dirPattern, String parentPath) throws IOException {
        // setup the working directory
        File workingDirectory = new File(source, parentPath);

        //If the directory doesn't exist, we should not archive it.
        if (workingDirectory.exists()) {
            // List, filter, and add the files
            File[] dirListing = workingDirectory.listFiles();
            if (dirListing != null) {
                for (int i = 0; i < dirListing.length; i++) {
                    File file = dirListing[i];

                    boolean includeFileInArchive;
                    if (dirPattern.getStrategy() == PatternStrategy.IncludePreference) {
                        includeFileInArchive = DirPattern.includePreference(file, dirPattern.getExcludePatterns(), dirPattern.getIncludePatterns(), dirPattern.includeByDefault);
                    } else {
                        includeFileInArchive = DirPattern.excludePreference(file, dirPattern.getExcludePatterns(), dirPattern.getIncludePatterns(), dirPattern.includeByDefault);
                    }

                    if (includeFileInArchive) {
                        // Add the entry
                        dirContent.add(parentPath + file.getName());
                    }

                    // Recurse into directories
                    if (file.isDirectory()) {
                        filterDirectory(dirContent, dirPattern, parentPath + file.getName() + "/");
                    }

                }
            }
        }

    }

}

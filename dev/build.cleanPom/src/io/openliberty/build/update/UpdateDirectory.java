/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.build.update;

import java.io.File;

import io.openliberty.build.update.util.Logger;

public class UpdateDirectory extends UpdateImpl {

    public UpdateDirectory(File rootDir, File tmpDir) {
        super(rootDir, tmpDir);
    }

    public UpdateDirectory(File rootDir, File tmpDir, boolean failOnError) {
        super(rootDir, tmpDir, failOnError);
    }

    public UpdateDirectory(File rootDir, File tmpDir, Logger logger) {
        super(rootDir, tmpDir, logger);
    }

    public UpdateDirectory(File rootDir, File tmpDir, Logger logger, boolean failOnError) {
        super(rootDir, tmpDir, logger, failOnError);
    }

    //

    public File getRootDir() {
        return getTargetFile();
    }

    //

    @Override
    public int run() throws Exception {
        String m = "run";

        logMark(m, "Starting ...");
        log(m, "  Input [ " + getTargetFile().getPath() + " ]");
        log(m, "  Temp  [ " + getTmpDir().getPath() + " ]");

        Stats useStats = getStats();
        useStats.reset();

        updateDir(getRootDir());

        logTime(m, "Completed:");
        log(m, "  Criteria  [ " + getCriteria() + " ]");
        log(m, "  All dirs  [ " + useStats.numDirs + " ]");
        log(m, "  All Files [ " + useStats.numFiles + " ]");
        log(m, "  Selected  [ " + useStats.numSelected + " ]");
        log(m, "  Updated   [ " + useStats.numUpdated + " ]");

        return useStats.numUpdated;
    }

    public static class Stats {
        public int numDirs;
        public int numFiles;
        public int numSelected;
        public int numUpdated;

        public void reset() {
            numDirs = 0;
            numFiles = 0;
            numSelected = 0;
            numUpdated = 0;
        }

        public void recordDir() {
            numDirs++;
        }

        public void recordUnselected() {
            numFiles++;
        }

        public void recordSelected() {
            numFiles++;
            numSelected++;
        }

        public void recordUpdated() {
            numFiles++;
            numSelected++;
            numUpdated++;
        }
    }

    private final Stats stats = new Stats();

    public Stats getStats() {
        return stats;
    }

    protected void recordDir() {
        stats.recordDir();
    }

    protected void recordUnselected() {
        stats.recordUnselected();
    }

    protected void recordSelected() {
        stats.recordSelected();
    }

    protected void recordUpdated() {
        stats.recordUpdated();
    }

    public void updateDir(File rootDir) throws Exception {
        String m = "updateDir";
        // log(m, "[ " + rootDir.getPath() + " ]");

        recordDir();

        File[] children = rootDir.listFiles();
        if (children == null) {
            // log(m, "Null children!");
            return;
        }

        for (File child : children) {
            if (child.isDirectory()) {
                updateDir(child);
            } else {
                if (select(child.getPath())) {
                    if (updateFile(child) == 1) {
                        log(m, "Updated [ " + child.getPath() + " ]");
                        recordUpdated();
                    } else {
                        log(m, "Not-updated [ " + child.getPath() + " ]");
                        recordSelected();
                    }
                } else {
                    // log(m, "Unselected [ " + child.getPath() + " ]");
                    recordUnselected();
                }
            }
        }
    }

    public String getCriteria() {
        return "*";
    }

    public boolean select(String targetPath) {
        return true;
    }

    public int updateFile(File childFile) throws Exception {
        String m = "updateFile";

        if (getFailOnError()) {
            UpdateFile updateFile = createChildUpdate(childFile);
            return updateFile.run();
        } else {
            try {
                UpdateFile updateFile = createChildUpdate(childFile);
                return updateFile.run();
            } catch (Exception e) {
                log(m, "Update failure: [ " + childFile.getAbsolutePath() + " ]:");
                e.printStackTrace();
                return 0;
            }
        }
    }

    public UpdateFile createChildUpdate(File childFile) throws Exception {
        return new UpdateFile(childFile, getTmpDir(), getLogger(), getFailOnError());
    }
}

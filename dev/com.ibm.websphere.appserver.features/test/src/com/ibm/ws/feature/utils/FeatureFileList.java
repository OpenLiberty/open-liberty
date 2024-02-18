/*******************************************************************************
 * Copyright (c) 2018, 2024 IBM Corporation and others.
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

package com.ibm.ws.feature.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class FeatureFileList implements Iterable<File> {

    private final String[] dirRoots;
    private final List<File> featureList = new ArrayList<File>();
    private boolean isInit = false;

    public FeatureFileList(String... roots) {
        this.dirRoots = roots;

    }

    public synchronized void populateList() {

        if (isInit)
            return;

        Queue<File> directories = new LinkedList<File>();

        for (String dirRoot : dirRoots) {
            File root = new File(dirRoot);

            directories.add(root);

            while (!directories.isEmpty()) {

                File nextDirectory = directories.poll();
                //System.out.println("inspecting directory: " + nextDirectory.getName());
                File[] nextDirectoryListing = nextDirectory.listFiles();
                if (nextDirectoryListing == null)
                    continue;

                for (File file : nextDirectoryListing) {
                    if (file.isDirectory()) { //Going down just one step.
                        for (File child : file.listFiles()) {
                            if (child.getName().endsWith(".feature")) {
                                this.featureList.add(child);
                            }
                        }
                        directories.add(file); //use this if we want to search every directory and subdirectory
                        //By adding the directories we find into the queue of all directories

                    }
                }
            }
        }

        isInit = true;
    }

    @Override
    public Iterator<File> iterator() {
        Iterator<File> itr = new Iterator<File>() {
            private int current = 0;

            @Override
            public void remove() {
                return;
            }

            @Override
            public boolean hasNext() {
                if (!isInit)
                    populateList();

                return ((current < featureList.size()) && !featureList.isEmpty());
            }

            @Override
            public File next() {
                if (!isInit)
                    populateList();

                return featureList.get(current++);
            }
        };

        return itr;

    }
}

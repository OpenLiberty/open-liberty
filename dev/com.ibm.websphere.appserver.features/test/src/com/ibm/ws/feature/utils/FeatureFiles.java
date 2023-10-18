/*******************************************************************************
 * Copyright (c) 2018,2023 IBM Corporation and others.
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class FeatureFiles {

    public FeatureFiles(File root) {
        Map<File, Set<File>> useCategories = new LinkedHashMap<>();
        Set<File> useAllChildren = collect(root, useCategories);

        this.root = root;
        this.categories = useCategories;
        this.allChildren = useAllChildren;

        this.autoChildren = this.categories.get(this.autoFile = new File(this.root, FeatureConstants.VISIBILITY_AUTO));
        this.privateChildren = this.categories.get(this.privateFile = new File(this.root, FeatureConstants.VISIBILITY_PRIVATE));
        this.protectedChildren = this.categories.get(this.protectedFile = new File(this.root, FeatureConstants.VISIBILITY_PROTECTED));
        this.publicChildren = this.categories.get(this.publicFile = new File(this.root, FeatureConstants.VISIBILITY_PUBLIC));
    }

    private final File root;

    private final File autoFile;
    private final File privateFile;
    private final File protectedFile;
    private final File publicFile;

    public File getRoot() {
        return root;
    }

    public File getAutoFile() {
        return autoFile;
    }

    public File getPrivateFile() {
        return privateFile;
    }

    public File getProtectedFile() {
        return protectedFile;
    }

    public File getPublicFile() {
        return publicFile;
    }

    public File getCategoryFile(String categoryName) {
        if (categoryName == null) {
            return null;
        } else if (categoryName.contentEquals(FeatureConstants.VISIBILITY_AUTO)) {
            return autoFile;
        } else if (categoryName.contentEquals(FeatureConstants.VISIBILITY_PRIVATE)) {
            return privateFile;
        } else if (categoryName.contentEquals(FeatureConstants.VISIBILITY_PROTECTED)) {
            return protectedFile;
        } else if (categoryName.contentEquals(FeatureConstants.VISIBILITY_PUBLIC)) {
            return publicFile;
        } else {
            return null;
        }
    }

    public Set<File> getCategory(String categoryName) {
        File categoryFile = getCategoryFile(categoryName);
        if (categoryFile == null) {
            return null;
        }
        return getCategory(categoryFile);
    }

    //

    private final Map<File, Set<File>> categories;

    private final Set<File> allChildren;

    private final Set<File> autoChildren;
    private final Set<File> privateChildren;
    private final Set<File> protectedChildren;
    private final Set<File> publicChildren;

    public Map<File, Set<File>> getCategories() {
        return categories;
    }

    public File getActualCategory(File featureFile) {
        if (autoChildren.contains(featureFile)) {
            return autoFile;
        } else if (privateChildren.contains(featureFile)) {
            return privateFile;
        } else if (protectedChildren.contains(featureFile)) {
            return protectedFile;
        } else if (publicChildren.contains(featureFile)) {
            return publicFile;
        } else {
            return null;
        }
    }

    public Set<File> getCategory(File file) {
        return getCategories().get(file);
    }

    public Set<File> getAllChildren() {
        return allChildren;
    }

    public Set<File> getAutoChildren() {
        return autoChildren;
    }

    public Set<File> getPrivateChildren() {
        return privateChildren;
    }

    public Set<File> getProtectedChildren() {
        return protectedChildren;
    }

    public Set<File> getPublicChildren() {
        return publicChildren;
    }

    //

    /**
     * Collect the feature files beneath a specified directory into categories.
     *
     * For each directory, collect both immediate and nested feature files.
     *
     * Store the overall collection for each directory in the categories table.
     *
     * Do not store empty collections.
     *
     * @param parent A parent directory.
     * @param categories Storage for feature files of that directory.
     *
     * @return The overall collection of feature files of the directory,
     *         including feature files in sub-directories.
     */
    protected static Set<File> collect(File parent, Map<File, Set<File>> categories) {
        Set<File> category = new LinkedHashSet<>();

        for (File child : parent.listFiles()) {
            if (child.isDirectory()) {
                // The category contains all nested feature files.
                category.addAll(FeatureFiles.collect(child, categories));
            } else if (child.getName().endsWith(FeatureFileConstants.FEATURE_FILE_EXT)) {
                // The category contains all immediate feature files.
                category.add(child);
            } else {
                // Ignore non-feature, non-directory children.
            }
        }

        // Do not store empty categories.
        if (!category.isEmpty()) {
            categories.put(parent, category);
        }

        return category;
    }
}

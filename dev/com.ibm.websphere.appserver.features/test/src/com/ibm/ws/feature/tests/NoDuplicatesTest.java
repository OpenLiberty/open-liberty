/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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

package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.ws.feature.tasks.FeatureBuilder;
import com.ibm.ws.feature.utils.FeatureFileList;

import org.junit.Assert;
import org.junit.Test;

public class NoDuplicatesTest {

    @Test
    public void testFeaturesHaveNoDuplicateBundles() {

        StringBuilder errorMessage = new StringBuilder();
        for (File featureFile : new FeatureFileList("./visibility/")) {
            FeatureBuilder featureBuilder = new FeatureBuilder();
            featureBuilder.setProperties(featureFile);

            // find duplicate entries in the bundle
            // bndtools handles duplicates by adding a "~" character to the end of the bundle name
            // https://github.com/bndtools/bnd/blob/64974c35b561f095433a0dc8d93f83be95c52b19/biz.aQute.bndlib/src/aQute/bnd/header/OSGiHeader.java#L105
            List<String> duplicateBundles = featureBuilder.getBundles()
                                                          .stream()
                                                          .map(bundle -> bundle.getKey())
                                                          .filter(bundle -> bundle.endsWith("~"))
                                                          // remove the ~ character after we find the duplicate bundles
                                                          .map(bundle -> bundle.substring(0, bundle.length() - 1))
                                                          .collect(Collectors.toList());

            if (!duplicateBundles.isEmpty()) {
                errorMessage.append(String.format("The feature file %s has duplicate bundles defined in the feature file: %s%n",
                                                  featureFile, duplicateBundles));
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features with duplicate bundles: \n" + errorMessage.toString());
        }
    }

    @Test
    public void testFeaturesHaveNoDuplicateFiles() {

        StringBuilder errorMessage = new StringBuilder();
        for (File featureFile : new FeatureFileList("./visibility/")) {
            FeatureBuilder featureBuilder = new FeatureBuilder();
            featureBuilder.setProperties(featureFile);

            // find duplicate entries in the files
            // bndtools handles duplicates by adding a "~" character to the end of the entry name
            // https://github.com/bndtools/bnd/blob/64974c35b561f095433a0dc8d93f83be95c52b19/biz.aQute.bndlib/src/aQute/bnd/header/OSGiHeader.java#L105
            List<String> duplicateFiles = featureBuilder.getFiles()
                                                        .stream()
                                                        .map(file -> file.getKey())
                                                        .filter(file -> file.endsWith("~"))
                                                        // remove the ~ character after we find the duplicate files
                                                        .map(file -> file.substring(0, file.length() - 1))
                                                        .collect(Collectors.toList());

            if (!duplicateFiles.isEmpty()) {
                errorMessage.append(String.format("The feature file %s has duplicate files defined in the feature file: %s%n",
                                                  featureFile, duplicateFiles));
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features with duplicate files: \n" + errorMessage.toString());
        }
    }

    @Test
    public void testFeaturesHaveNoDuplicateJars() {

        StringBuilder errorMessage = new StringBuilder();
        for (File featureFile : new FeatureFileList("./visibility/")) {
            FeatureBuilder featureBuilder = new FeatureBuilder();
            featureBuilder.setProperties(featureFile);

            // find duplicate entries in the jars
            // bndtools handles duplicates by adding a "~" character to the end of the entry name
            // https://github.com/bndtools/bnd/blob/64974c35b561f095433a0dc8d93f83be95c52b19/biz.aQute.bndlib/src/aQute/bnd/header/OSGiHeader.java#L105
            List<String> duplicateJars = featureBuilder.getJars()
                                                       .stream()
                                                       .map(jar -> jar.getKey())
                                                       .filter(jar -> jar.endsWith("~"))
                                                       // remove the ~ character after we find the duplicate jars
                                                       .map(jar -> jar.substring(0, jar.length() - 1))
                                                       .collect(Collectors.toList());

            if (!duplicateJars.isEmpty()) {
                errorMessage.append(String.format("The feature file %s has duplicate jars defined in the feature file: %s%n",
                                                  featureFile, duplicateJars));
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features with duplicate jars: \n" + errorMessage.toString());
        }
    }

    @Test
    public void testFeaturesHaveNoDuplicateFeatures() {

        StringBuilder errorMessage = new StringBuilder();
        for (File featureFile : new FeatureFileList("./visibility/")) {
            FeatureBuilder featureBuilder = new FeatureBuilder();
            featureBuilder.setProperties(featureFile);

            // find duplicate entries in the features
            // bndtools handles duplicates by adding a "~" character to the end of the entry name
            // https://github.com/bndtools/bnd/blob/64974c35b561f095433a0dc8d93f83be95c52b19/biz.aQute.bndlib/src/aQute/bnd/header/OSGiHeader.java#L105
            List<String> duplicateFeatures = featureBuilder.getFeatures()
                                                           .stream()
                                                           .map(feature -> feature.getKey())
                                                           .filter(feature -> feature.endsWith("~"))
                                                           // remove the ~ character after we find the duplicate features
                                                           .map(feature -> feature.substring(0, feature.length() - 1))
                                                           .collect(Collectors.toList());

            if (!duplicateFeatures.isEmpty()) {
                errorMessage.append(String.format("The feature file %s has duplicate features defined in the feature file: %s%n",
                                                  featureFile, duplicateFeatures));
            }
        }

        if (errorMessage.length() != 0) {
            Assert.fail("Found features with duplicate features: \n" + errorMessage.toString());
        }
    }
}

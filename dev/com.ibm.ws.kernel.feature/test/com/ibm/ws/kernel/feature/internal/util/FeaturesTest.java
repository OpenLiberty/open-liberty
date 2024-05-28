/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.kernel.feature.internal.util;

import java.io.File;
import java.util.List;

import org.junit.Test;

import junit.framework.Assert;

public class FeaturesTest {
    public static final String FEATURES_PROJECT_PATH = "../com.ibm.websphere.appserver.features";
    public static final String FEATURES_PATH = FEATURES_PROJECT_PATH + "/" + "visibility";

    public static final String FEATURES_OUTPUT_PATH = "./build/features/features.xml";

    @Test
    public void testPrint() throws Exception {
        File rootFeaturesInputFile = new File(FEATURES_PATH);
        String rootInputPath = rootFeaturesInputFile.getAbsolutePath();
        if (!rootFeaturesInputFile.exists()) {
            Assert.fail("Features input [ " + rootInputPath + " ] does not exist");
        } else {
            System.out.println("Features input [ " + rootInputPath + " ]");
        }

        File featuresOutputFile = new File(FEATURES_OUTPUT_PATH);
        String featuresOutputPath = featuresOutputFile.getAbsolutePath();
        File outputParent = featuresOutputFile.getParentFile();
        String outputParentPath = outputParent.getAbsolutePath();
        outputParent.mkdirs();
        if (!outputParent.exists()) {
            Assert.fail("Features output [ " + outputParentPath + " ] does not exist");
        } else if (!outputParent.isDirectory()) {
            Assert.fail("Features output [ " + outputParentPath + " ] is not a directory");
        } else {
            System.out.println("Features output [ " + featuresOutputPath + " ]");
        }

        List<File> featureFiles = FeatureReader.selectFeatureFiles(rootFeaturesInputFile);
        System.out.println("Selected [ " + featureFiles.size() + " ] feature files");

        List<FeatureInfo> features = FeatureReader.readFeatures(featureFiles);
        System.out.println("Read [ " + features.size() + " ] features from [ " + rootInputPath + " ]");

        FeatureXML.sort(features);
        FeatureXML.write(featuresOutputFile, features);
        System.out.println("Wrote [ " + features.size() + " ] features to [ " + featuresOutputPath + " ]");
    }
}

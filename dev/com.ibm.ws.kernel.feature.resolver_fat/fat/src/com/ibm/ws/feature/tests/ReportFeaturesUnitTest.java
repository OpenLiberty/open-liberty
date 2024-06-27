/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.tests.util.FeatureCategories;
import com.ibm.ws.feature.tests.util.FeatureInfo;
import com.ibm.ws.feature.tests.util.FeatureXML;
import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

import junit.framework.Assert;

public class ReportFeaturesUnitTest {
    public static final String FEATURES_XML_OUTPUT_PATH = "./build/features/features.xml";
    public static final String FEATURES_TEXT_OUTPUT_PATH = "./build/features/features.text";

    public static final String SERVER_NAME = "FeatureResolverTest";

    //

    private static boolean didSetup;

    @BeforeClass
    public static void setupClass() throws Exception {
        if (didSetup) {
            return;
        } else {
            didSetup = true;
        }

        RepositoryUtil.setupFeatures();
        RepositoryUtil.setupRepo(SERVER_NAME);
    }

    protected static File setupOutput(String tag, String outputPath) throws Exception {
        File outputFile = new File(outputPath);
        String absOutputPath = outputFile.getCanonicalPath();

        File outputParent = outputFile.getParentFile();
        String absOutputParentPath = outputParent.getCanonicalPath();

        outputParent.mkdirs();

        if (!outputParent.exists()) {
            Assert.fail(tag + " [ " + absOutputParentPath + " ] does not exist");
        } else if (!outputParent.isDirectory()) {
            Assert.fail(tag + " [ " + absOutputParentPath + " ] is not a directory");
        } else {
            System.out.println("tag [ " + absOutputPath + " ]");
        }

        return new File(absOutputPath);
    }

    @Test
    public void report_featureInfoTest() throws Exception {
        File xmlOutputFile = setupOutput("Features output", FEATURES_XML_OUTPUT_PATH);

        List<FeatureInfo> features = RepositoryUtil.getFeaturesList();

        System.out.println("Writing [ " + features.size() + " ] features to [ " + xmlOutputFile.getPath() + " ] ...");
        FeatureXML.sort(features);
        FeatureXML.write(xmlOutputFile, features);
        System.out.println("Wrote [ " + features.size() + " ] features to [ " + xmlOutputFile.getPath() + " ]");
    }

    @Test
    public void report_featureDefsTest() throws Exception {
        File textOutputFile = setupOutput("Features output", FEATURES_TEXT_OUTPUT_PATH);

        List<ProvisioningFeatureDefinition> featureDefs = RepositoryUtil.getFeatureDefs();

        System.out.println("Writing [ " + featureDefs.size() + " ] features to [ " + textOutputFile.getPath() + " ] ...");
        FeatureCategories featureCategories = new FeatureCategories(featureDefs);
        featureCategories.write(textOutputFile);
        System.out.println("Wrote [ " + featureDefs.size() + " ] features to [ " + textOutputFile.getPath() + " ]");
    }

}

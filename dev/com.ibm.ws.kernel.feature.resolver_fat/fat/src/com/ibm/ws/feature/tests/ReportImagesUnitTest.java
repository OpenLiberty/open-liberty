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

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.tests.util.RepositoryUtil;
import com.ibm.ws.kernel.feature.internal.util.ImageXML;
import com.ibm.ws.kernel.feature.internal.util.Images;

import junit.framework.Assert;

public class ReportImagesUnitTest {
    public static final String IMAGES_OUTPUT_PATH = "./build/images/images.xml";

    private static boolean didSetup;

    @BeforeClass
    public static void setupClass() throws Exception {
        if (didSetup) {
            return;
        } else {
            didSetup = true;
        }

        RepositoryUtil.setupProfiles();
    }

    @Test
    public void report_imagesTest() throws Exception {
        File imagesOutputFile = new File(IMAGES_OUTPUT_PATH);
        String imagesOutputPath = imagesOutputFile.getCanonicalPath();
        File outputParent = imagesOutputFile.getParentFile();
        String outputParentPath = outputParent.getCanonicalPath();
        outputParent.mkdirs();
        if (!outputParent.exists()) {
            Assert.fail("Images output [ " + outputParentPath + " ] does not exist");
        } else if (!outputParent.isDirectory()) {
            Assert.fail("Images output [ " + outputParentPath + " ] is not a directory");
        } else {
            System.out.println("Images output [ " + imagesOutputPath + " ]");
        }

        Images images = RepositoryUtil.getImages();

        ImageXML.write(imagesOutputFile, images);
        System.out.println("Wrote [ " + images.getImages().size() + " ] images to [ " + imagesOutputPath + " ]");
    }
}

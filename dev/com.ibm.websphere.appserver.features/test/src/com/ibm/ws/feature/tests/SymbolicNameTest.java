/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.feature.tests;

import java.io.File;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureFileList;
import com.ibm.ws.feature.utils.FeatureInfo;

public class SymbolicNameTest {

    private static FeatureFileList featureFileList = null;

	@BeforeClass
	public static void setUpClass() {
	    featureFileList = new FeatureFileList("./visibility/");
	}

	@Test
	public void testSymbolicName() {
	    for (File featureFile : featureFileList) {
            FeatureInfo featureInfo = new FeatureInfo(featureFile);
            String symbolicName = featureInfo.getName();
            String fileName = featureFile.getName();
            int index = fileName.lastIndexOf(".feature");
            if (index != -1) {
                fileName = fileName.substring(0, index);
            }
            Assert.assertEquals("symbolicName doesn't match the name of the file", fileName, symbolicName);
	    }
	}
}

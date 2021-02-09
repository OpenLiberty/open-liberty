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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * Validates that if multiple features start with the same prefix that they are appropriately
	 * marked as singletons so that they can't both load in the same server.
	 */
	@Test
	public void testSingleton() {
	    Map<String, Map<String, List<FeatureInfo>>> featureMap = new HashMap<>();
        for (File featureFile : featureFileList) {
            FeatureInfo featureInfo = new FeatureInfo(featureFile);
            if (featureInfo.isAutoFeature()) {
                continue;
            }
            String symbolicName = featureInfo.getName();
            // javaeePlatform and appSecurity are special because they have dependencies on each other.
            if (symbolicName.startsWith("com.ibm.websphere.appserver.javaeePlatform") ||
                    symbolicName.startsWith("com.ibm.websphere.appserver.appSecurity")) {
                continue;
            }

            int versionIndex = symbolicName.lastIndexOf('-');
            String nameWithoutVersion;
            if (versionIndex != -1) {
                nameWithoutVersion = symbolicName.substring(0, versionIndex);
            } else {
                nameWithoutVersion = symbolicName;
            }
            Map<String, List<FeatureInfo>> featureListMap = featureMap.get(nameWithoutVersion);
            if (featureListMap == null) {
                featureListMap = new HashMap<>();
                featureMap.put(nameWithoutVersion, featureListMap);
            }
            String visibility = featureInfo.getVisibility();
            List<FeatureInfo> featureList = featureListMap.get(visibility);
            if (featureList == null) {
                featureList = new ArrayList<>();
                featureListMap.put(visibility, featureList);
            }
            featureList.add(featureInfo);
        }
        StringBuilder errorMessage = new StringBuilder();
        for (Map<String, List<FeatureInfo>> featureListMap : featureMap.values()) {
            for (List<FeatureInfo> featureList : featureListMap.values()) {
                if (featureList.size() > 1) {
                    for (FeatureInfo featureInfo : featureList) {
                        if (!featureInfo.isSingleton()) {
                            errorMessage.append("Found issues with " + featureInfo.getName() + '\n');
                            errorMessage.append("     There are other versions with the same name and it isn't marked as a singleton: " + '\n');
                        }
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that aren't correctly marked as singletons: " + '\n' + errorMessage.toString());
        }
	}
}

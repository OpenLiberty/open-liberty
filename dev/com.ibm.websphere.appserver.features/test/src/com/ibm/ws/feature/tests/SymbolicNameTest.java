/*******************************************************************************
 * Copyright (c) 2020,2025 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.feature.utils.FeatureFileList;
import com.ibm.ws.feature.utils.FeatureInfo;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;

public class SymbolicNameTest {

    private static FeatureFileList featureFileList = null;

    @BeforeClass
    public static void setUpClass() {
        featureFileList = new FeatureFileList("./visibility/");
    }

    @Test
    public void testSymbolicName() {
        StringBuilder errorMessage = new StringBuilder();
        for (File featureFile : featureFileList) {
            FeatureInfo featureInfo = new FeatureInfo(featureFile);
            String symbolicName = featureInfo.getName();
            String fileName = featureFile.getName();
            int index = fileName.lastIndexOf(".feature");
            if (index != -1) {
                fileName = fileName.substring(0, index);
            }
            if (!fileName.equals(symbolicName)) {
                errorMessage.append("Symbolic name ").append(symbolicName).append(" doesn't match the name of the file ").append(fileName).append('\n');
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features where symbolic name doesn't match the file name: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * Validates that if multiple features start with the same prefix that they are appropriately marked as singletons
     * so that they can't both load in the same server.
     */
    @Test
    public void testSingleton() {
        StringBuilder errorMessage = new StringBuilder();
        Map<String, Map<String, List<FeatureInfo>>> featureMap = new HashMap<>();
        for (File featureFile : featureFileList) {
            FeatureInfo featureInfo = new FeatureInfo(featureFile);
            if (featureInfo.isAutoFeature()) {
                if (featureInfo.isSingleton()) {
                    errorMessage.append("Found issues with " + featureInfo.getName() + '\n');
                    errorMessage.append("     Auto features should not be marked as singleton" + '\n');
                }
                continue;
            }
            String symbolicName = featureInfo.getName();
            // javaeePlatform and appSecurity are special because they have dependencies on each other.
            if (symbolicName.startsWith("io.openliberty.jakartaeePlatform")
                || symbolicName.startsWith("com.ibm.websphere.appserver.javaeePlatform")
                || symbolicName.startsWith("com.ibm.websphere.appserver.appSecurity")) {
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
        for (Map<String, List<FeatureInfo>> featureListMap : featureMap.values()) {
            for (List<FeatureInfo> featureList : featureListMap.values()) {
                if (featureList.size() > 1) {
                    for (FeatureInfo featureInfo : featureList) {
                        if (!featureInfo.isSingleton()) {
                            errorMessage.append("Found issues with " + featureInfo.getName() + '\n');
                            errorMessage.append(
                                                "     There are other versions with the same name and it isn't marked as a singleton: "
                                                + '\n');
                        }
                    }
                }
            }
        }
        if (errorMessage.length() != 0) {
            Assert.fail("Found features that aren't correctly marked as singletons: " + '\n' + errorMessage.toString());
        }
    }

    /**
     * When exporting bundles and features in gradle-bootstrap, if a feature and bundle have the same name, they will conflict. They have different
     * versions and different types, esa vs jar, but there is only one pom file with the same name so it won't work if a bundle matches a feature name.
     */
    @Test
    public void testBundleFeatureConflict() throws Exception {
        StringBuilder errorMessage = new StringBuilder();
        Set<String> symbolicNames = new HashSet<>();
        for (File featureFile : featureFileList) {
            FeatureInfo featureInfo = new FeatureInfo(featureFile);
            symbolicNames.add(featureInfo.getName());
        }
        File workspaceDir = new File(System.getProperty("user.dir")).getAbsoluteFile().getParentFile();

        Workspace bndWorkspace = new Workspace(workspaceDir, "cnf");
        bndWorkspace.setOffline(true);
        Collection<Project> projects = bndWorkspace.getAllProjects();
        for (Project project : projects) {
            Collection<String> bsns = project.getBsns();
            for (String bsn : bsns) {
                if (symbolicNames.contains(bsn)) {
                    errorMessage.append(bsn).append(" in project ").append(project.getName()).append('\n');
                }
            }
        }
        bndWorkspace.close();

        if (errorMessage.length() != 0) {
            Assert.fail("Found features where symbolic name matches a bundle name: " + '\n' + errorMessage.toString());
        }
    }
}

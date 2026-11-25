/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.suite.staging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.ws.test.image.build.BuildImages.ImageType;

public class InstallBundlesData {
    public static final List<Object[]> TEST_DATA;
    
    static {
        List<Object[]> testData = new ArrayList<Object[]>(3);

        testData.add( new Object[] {
                "core",
                ImageType.CORE_LIC, ImageType.CORE_ALL,
                Collections.singletonList("libertyCoreBundle") } );

        testData.add( new Object[] {
                "base",
                ImageType.BASE_LIC, ImageType.BASE_ALL,
                Collections.singletonList("baseBundle") } );

        List<String> ndBundles = new ArrayList<>(2);
        ndBundles.add("ndMemberBundle");
        ndBundles.add("ndControllerBundle");

        testData.add( new Object[] {
                "nd",
                ImageType.ND_LIC, ImageType.ND_ALL,
                ndBundles } );

        TEST_DATA = testData;
    }

    // WS-CD-Open Bundles:
    //
    // WS-CD-Open/dev/
    //   com.ibm.websphere.appserver.baseBundle
    //   com.ibm.websphere.appserver.jakartaee9Bundle
    //   com.ibm.websphere.appserver.javaee7Bundle
    //   com.ibm.websphere.appserver.javaee8Bundle
    //   com.ibm.websphere.appserver.libertyCoreBundle
    //   com.ibm.websphere.appserver.ndControllerBundle
    //   com.ibm.websphere.appserver.ndMemberBundle
    //   com.ibm.websphere.appserver.osgiBundle-1.0
    //   com.ibm.websphere.appserver.webProfile7Bundle
    //   com.ibm.websphere.appserver.webProfile8Bundle
    //   com.ibm.websphere.appserver.webProfile9Bundle
    //   com.ibm.websphere.appserver.zosBundle
    //   com.ibm.websphere.appserver.zosCoreBundle

    // Image repository files (repo.xml) are in:
    //  WS-CD-Open/dev/build.image/editions/
    //    base
    //    core
    //    nd
    //    zos
    
    public static final String[] BUNDLE_NAMES = new String[] {
            "baseBundle",
            "jakartaee9Bundle",
            "javaee7Bundle",
            "javaee8Bundle",
            "libertyCoreBundle",
            "ndControllerBundle",
            "ndMemberBundle",
            "osgiBundle-1.0",
            "webProfile7Bundle",
            "webProfile8Bundle",
            "webProfile9Bundle",
            "zosBundle",
            "zosCoreBundle"
    };
    
    public static final String BUNDLE_PREFIX = "com.ibm.websphere.appserver.";
    public static final String BUNDLE_SUFFIX = ".feature";

    public static String getBundleLocation(String bundleName) {
        String bundleFullName = BUNDLE_PREFIX + bundleName;
        return bundleFullName + "/" + bundleFullName + BUNDLE_SUFFIX;
    }
}

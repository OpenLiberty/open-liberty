/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests.util;

import com.ibm.ws.kernel.feature.Visibility;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

public class FeatureUtil {

    public static boolean isPublic(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getVisibility() == Visibility.PUBLIC);
    }

    public static boolean isProtected(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getVisibility() == Visibility.PROTECTED);
    }

    public static boolean isPrivate(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getVisibility() == Visibility.PRIVATE);
    }

    public static boolean isAuto(ProvisioningFeatureDefinition featureDef) {
        return featureDef.isAutoFeature();
    }

    public static boolean isConvenience(ProvisioningFeatureDefinition featureDef) {
        if (!isPublic(featureDef)) {
            return false;
        } else if ((featureDef.getIbmShortName() == null)) {
            return false;
        }

        // Include ".appserver." to avoid "io.openliberty.securityAPI.javaee-1.0"

        String symName = featureDef.getSymbolicName();
        return (symName.contains(".appserver.javaee-") ||
                symName.contains(".jakartaee-") ||
                symName.contains(".microProfile-"));
    }

    public static boolean isCompatibility(ProvisioningFeatureDefinition featureDef) {
        if (!isPrivate(featureDef)) {
            return false;
        } else if (featureDef.getIbmShortName() != null) {
            return false;
        }

        return (featureDef.getPlatformName() != null);

        // String symName = featureDef.getSymbolicName();
        // return (symName.contains(".eeCompatible-") ||
        //         symName.contains(".mpVersion-"));
    }

    public static boolean isVersionless(ProvisioningFeatureDefinition featureDef) {
        if (featureDef.isAutoFeature()) {
            return false;
        } else if (!isPublic(featureDef)) {
            return false;
        }

        String shortName = featureDef.getIbmShortName();
        if ((shortName == null) || !shortName.equals(featureDef.getFeatureName())) {
            return false;
        }
        if (featureDef.getSymbolicName().indexOf(".versionless.") == -1) {
            return false;
        }

        return true;
    }

    public static boolean isVersionlessLink(ProvisioningFeatureDefinition featureDef) {
        return (featureDef.getSymbolicName().contains(".internal.versionless."));
    }

    public static String getVersionLink(ProvisioningFeatureDefinition featureDef) {
        String symName = featureDef.getSymbolicName();
        int tailOffset = symName.indexOf(".internal.versionless.");
        if (tailOffset == -1) {
            return null;
        }
        tailOffset += ".internal.versionless.".length();
        return symName.substring(tailOffset);
    }

//  com.ibm.websphere.appserver.eeCompatible-6.0.mf
//  com.ibm.websphere.appserver.javaeePlatform-6.0
//
//  com.ibm.websphere.appserver.javaee-7.0
//  com.ibm.websphere.appserver.javaeePlatform-7.0
//  com.ibm.websphere.appserver.eeCompatible-7.0.mf
//
//  com.ibm.websphere.appserver.javaee-8.0
//  com.ibm.websphere.appserver.javaeePlatform-8.0
//  com.ibm.websphere.appserver.eeCompatible-8.0.mf
//
//  io.openliberty.jakartaee-9.1
//  io.openliberty.jakartaeePlatform-9.0
//  com.ibm.websphere.appserver.eeCompatible-9.0.mf
//
//  io.openliberty.jakartaee-10.0
//  io.openliberty.jakartaeePlatform-10.0
//  com.ibm.websphere.appserver.eeCompatible-10.0.mf
//
//  io.openliberty.jakartaee-11.0
//  io.openliberty.jakartaeePlatform-11.0
//  com.ibm.websphere.appserver.eeCompatible-11.0.mf
//
//  io.openliberty.internal.mpVersion-1.0
//  com.ibm.websphere.appserver.microProfile-1.0
//
//  com.ibm.websphere.appserver.microProfile-1.2
//  io.openliberty.internal.mpVersion-1.2
//
//  com.ibm.websphere.appserver.microProfile-1.3
//  io.openliberty.internal.mpVersion-1.3
//
//  com.ibm.websphere.appserver.microProfile-1.4
//  io.openliberty.internal.mpVersion-1.4
//
//  com.ibm.websphere.appserver.microProfile-2.0
//  io.openliberty.internal.mpVersion-2.0
//
//  com.ibm.websphere.appserver.microProfile-2.1
//  io.openliberty.internal.mpVersion-2.1
//
//  com.ibm.websphere.appserver.microProfile-2.2
//  io.openliberty.internal.mpVersion-2.2
//
//  com.ibm.websphere.appserver.microProfile-3.0
//  io.openliberty.internal.mpVersion-3.0
//
//  com.ibm.websphere.appserver.microProfile-3.2
//  io.openliberty.internal.mpVersion-3.2
//
//  com.ibm.websphere.appserver.microProfile-3.3
//  io.openliberty.internal.mpVersion-3.3
//
//  com.ibm.websphere.appserver.microProfile-4.0
//  io.openliberty.internal.mpVersion-4.0
//  io.openliberty.mpCompatible-4.0.mf
//
//  com.ibm.websphere.appserver.microProfile-4.1
//  io.openliberty.internal.mpVersion-4.1
//
//  io.openliberty.microProfile-5.0
//  io.openliberty.internal.mpVersion-5.0
//  io.openliberty.mpCompatible-5.0.mf
//
//  io.openliberty.microProfile-6.0
//  io.openliberty.internal.mpVersion-6.0
//  io.openliberty.mpCompatible-6.0.mf
//
//  io.openliberty.microProfile-6.1
//  io.openliberty.internal.mpVersion-6.1
//  io.openliberty.mpCompatible-6.1.mf

    //  io.openliberty.internal.mpVersion-1.0
    //  io.openliberty.internal.mpVersion-1.2
    //  io.openliberty.internal.mpVersion-1.3
    //  io.openliberty.internal.mpVersion-1.4
    //  io.openliberty.internal.mpVersion-2.0
    //  io.openliberty.internal.mpVersion-2.1
    //  io.openliberty.internal.mpVersion-2.2
    //  io.openliberty.internal.mpVersion-3.0
    //  io.openliberty.internal.mpVersion-3.2
    //  io.openliberty.internal.mpVersion-3.3
    //  io.openliberty.internal.mpVersion-4.0
    //  io.openliberty.internal.mpVersion-4.1
    //  io.openliberty.internal.mpVersion-5.0
    //  io.openliberty.internal.mpVersion-6.0
    //  io.openliberty.internal.mpVersion-6.1

    public static void parseName(String symName, String[] parts) {
        String baseName;
        String version;
        int lastDash = symName.lastIndexOf('-');
        if (lastDash >= 0) {
            baseName = symName.substring(0, lastDash);
            version = symName.substring(lastDash + 1);
        } else {
            baseName = symName;
            version = null;
        }
        parts[0] = baseName;
        parts[1] = version;
    }

    public static String getBaseName(String symName) {
        int lastDash = symName.lastIndexOf('-');
        return ((lastDash >= 0) ? symName.substring(0, lastDash) : symName);
    }

    public static String getVersion(String symName) {
        int lastDash = symName.lastIndexOf('-');
        return ((lastDash >= 0) ? symName.substring(lastDash + 1) : null);
    }

    public static String getSymbolicName(String baseName, String version) {
        return ((version == null) ? baseName : (baseName + "-" + version));
    }

    public static String getShortName(String baseName) {
        int lastDot = baseName.lastIndexOf('.');
        return ((lastDot >= 0) ? baseName.substring(lastDot + 1) : baseName);
    }

    /**
     * Tests are placing several test related features in the server features
     * repository. Ignore these for now:
     *
     * <ul>
     * <li>test.InterimFixManagerTest-1.0.mf</li>
     * <li>test.InterimFixesManagerTest-1.0.mf</li>
     * <li>test.TestFixManagerTest-1.0.mf</li>
     * <li>test.featurefixmanager-1.0.mf</li>
     * <li>features with IBM-Test-Feature:true set</li>
     * </ul>
     *
     * @param featureName A feature name.
     *
     * @return True or false telling if the named feature is a test feature.
     */
    public static boolean isTest(String featureName, ProvisioningFeatureDefinition featureDef) {
        return featureName.startsWith("test.") || "true".equals(featureDef.getHeader("IBM-Test-Feature"));
    }
}

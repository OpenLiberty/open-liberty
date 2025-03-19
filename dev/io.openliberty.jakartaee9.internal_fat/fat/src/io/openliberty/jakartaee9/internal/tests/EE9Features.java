/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee9.internal.tests;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.FeatureUtilities;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.JavaInfo;

public class EE9Features {

    public static final boolean OPEN_LIBERTY_ONLY = true;

    public static Set<String> getInstalledFeatures(String installRoot, boolean openLibertyOnly) {
        return new HashSet<String>(FeatureUtilities.getFeaturesFromServer(new File(installRoot), openLibertyOnly));
    }

    public static Set<String> getVersionedFeatures(Set<String> features) {
        return FeatureUtilities.rejectVersionless(features);
    }

    private static final Set<String> EMPTY_STRINGS = Collections.<String> emptySet();

    public static String getEE9Replacement(String feature) {
        return FeatureReplacementAction.getReplacementFeature(feature,
                                                              JakartaEE9Action.EE9_FEATURE_SET,
                                                              EMPTY_STRINGS);
    }

    public static final Set<String> getTestFeatures() {
        return FeatureUtilities.allTestFeatures();
    }

    public static final Set<String> getEEFeatures(boolean openLiberty) {
        return FeatureUtilities.allEeFeatures(openLiberty);
    }

    public static final Set<String> getMPFeatures() {
        return FeatureUtilities.allMpFeatures();
    }

    public static final Set<String> getCompatibleMPFeatures(EEVersion version) {
        return FeatureUtilities.compatibleMpFeatures(version);
    }

    //

    public static Map<String, String> getServletConflicts() {
        Map<String, String> conflicts = new HashMap<>(6);
        conflicts.put("servlet-6.1", "com.ibm.websphere.appserver.servlet");
        conflicts.put("servlet-6.0", "com.ibm.websphere.appserver.servlet");
        conflicts.put("servlet-5.0", "com.ibm.websphere.appserver.servlet");
        conflicts.put("servlet-4.0", "com.ibm.websphere.appserver.servlet");
        conflicts.put("servlet-3.1", "com.ibm.websphere.appserver.servlet");
        conflicts.put("servlet-3.0", "com.ibm.websphere.appserver.servlet");
        return conflicts;
    }

    public static Map<String, String> getCDIConflicts() {
        Map<String, String> conflicts = new HashMap<>(3);
        conflicts.put("cdi-3.0", "io.openliberty.cdi");
        conflicts.put("cdi-4.0", "io.openliberty.cdi");
        conflicts.put("cdi-4.1", "io.openliberty.cdi");
        return conflicts;
    }

    //

    public static Set<String> getEE7ConflictFeatures() {
        Set<String> features = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);

        features.removeAll(getTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        features.remove("j2eeManagement-1.1");

        // A couple of special cases that we want to make sure work.
        features.add("appSecurity-1.0");
        features.add("websocket-1.0");

        // servlet long name is the same for EE9 so it will fail because the prefixes
        // match and it is marked as a singleton.
        features.remove("servlet-3.1");

        return features;
    }

    public static Set<String> getEE8ConflictFeatures() {
        Set<String> features = new HashSet<>(EE8FeatureReplacementAction.EE8_FEATURE_SET);

        features.removeAll(getTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 so there is no replacement
        features.remove("j2eeManagement-1.1");

        // servlet long name is the same for EE9 so it will fail because the prefixes
        // match and it is marked as a singleton.
        features.remove("servlet-4.0");

        return features;
    }

    //

    public EE9Features(String installRoot) {
        FeatureUtilities.removeTestAutoFeatures(new File(installRoot));
        this.serverFeatures_ol = getInstalledFeatures(installRoot, OPEN_LIBERTY_ONLY);
        this.versionedFeatures_ol = getVersionedFeatures(serverFeatures_ol);

        this.compatibleFeatures_ol = getCompatibleFeatures(versionedFeatures_ol, OPEN_LIBERTY_ONLY);
        this.extendedCompatibleFeatures_ol = getExtendedCompatibleFeatures(compatibleFeatures_ol, OPEN_LIBERTY_ONLY);

        this.incompatibleFeatures_ol = getIncompatibleFeatures(versionedFeatures_ol,
                                                               compatibleFeatures_ol,
                                                               OPEN_LIBERTY_ONLY);

        this.serverFeatures_wl = getInstalledFeatures(installRoot, !OPEN_LIBERTY_ONLY);
        this.versionedFeatures_wl = getVersionedFeatures(serverFeatures_wl);

        this.compatibleFeatures_wl = getCompatibleFeatures(versionedFeatures_wl, !OPEN_LIBERTY_ONLY);
        this.extendedCompatibleFeatures_wl = getExtendedCompatibleFeatures(compatibleFeatures_wl, !OPEN_LIBERTY_ONLY);

        this.incompatibleFeatures_wl = getIncompatibleFeatures(versionedFeatures_wl,
                                                               compatibleFeatures_wl,
                                                               !OPEN_LIBERTY_ONLY);
    }

    //

    private final Set<String> serverFeatures_ol;
    private final Set<String> versionedFeatures_ol;
    private final Set<String> compatibleFeatures_ol;
    private final Set<String> extendedCompatibleFeatures_ol;
    private final Set<String> incompatibleFeatures_ol;

    private final Set<String> serverFeatures_wl;
    private final Set<String> versionedFeatures_wl;
    private final Set<String> compatibleFeatures_wl;
    private final Set<String> extendedCompatibleFeatures_wl;
    private final Set<String> incompatibleFeatures_wl;

    public Set<String> getServerFeatures(boolean openLiberty) {
        return openLiberty ? serverFeatures_ol : serverFeatures_wl;
    }

    public Set<String> getVersionedFeatures(boolean openLiberty) {
        return openLiberty ? versionedFeatures_ol : versionedFeatures_wl;
    }

    public Set<String> getCompatibleFeatures(boolean openLiberty) {
        return openLiberty ? compatibleFeatures_ol : compatibleFeatures_wl;
    }

    public Set<String> getExtendedCompatibleFeatures(boolean openLiberty) {
        return openLiberty ? extendedCompatibleFeatures_ol : extendedCompatibleFeatures_wl;
    }

    public Set<String> getIncompatibleFeatures(boolean openLiberty) {
        return openLiberty ? incompatibleFeatures_ol : incompatibleFeatures_wl;
    }

    //

    private static Set<String> getCompatibleFeatures(Set<String> versionedFeatures, boolean openLiberty) {
        Set<String> features = new HashSet<>();

        // By default, features are assumed to be compatible
        features.addAll(versionedFeatures);

        // Any features from a different EE are *not* compatibile.
        // Remove all of the EE features, then add back the EE9 features.
        features.removeAll(getEEFeatures(openLiberty));
        features.addAll(JakartaEE9Action.EE9_FEATURE_SET);

        // Most MicroProfile features are only compatible with specific EE versions.
        // Remove all MicroProfile features then add back the ones known to be
        // valid for EE9.
        features.removeAll(getMPFeatures());
        features.addAll(getCompatibleMPFeatures(EEVersion.EE9));

        // mpTelemetry-1.1 and 2.0 are compatible with EE9 but not in Mp50.
        features.add("mpTelemetry-1.1");
        features.add("mpTelemetry-2.0");

        // mpReactive features are not included on Java 8, but will resolve because Java version
        // is not included in resolution.
        features.add("mpReactiveStreams-3.0");
        features.add("mpReactiveMessaging-3.0");

        // Value-add features which aren't compatible
        features.remove("openid-2.0"); // stabilized
        features.remove("openapi-3.1"); // depends on mpOpenAPI
        features.remove("opentracing-1.0"); // opentracing depends on mpConfig
        features.remove("opentracing-1.1");
        features.remove("opentracing-1.2");
        features.remove("opentracing-1.3");
        features.remove("opentracing-2.0");
        features.remove("sipServlet-1.1"); // purposely not supporting EE 9
        features.remove("springBoot-1.5");
        features.remove("springBoot-2.0");
        features.remove("springBoot-3.0"); // springBoot 3.0 only supports EE 10

        if (!openLiberty) {
            // These stabilized features are not compatible with EE9.
            features.remove("apiDiscovery-1.0");
            features.remove("blueprint-1.0");
            features.remove("httpWhiteboard-1.0");
            features.remove("mqtt-3.1");
            features.remove("openapi-3.0");
            features.remove("osgiAppConsole-1.0");
            features.remove("osgiAppIntegration-1.0");
            features.remove("osgiBundle-1.0");
            features.remove("osgi.jpa-1.0");
            features.remove("restConnector-1.0");
            features.remove("rtcomm-1.0");
            features.remove("rtcommGateway-1.0");
            features.remove("scim-1.0");
            features.remove("wab-1.0");
            features.remove("zosConnect-1.0");
            features.remove("zosConnect-1.2");

            // These depend on previous EE versions;
            // EE9 uses 'wmqMessagingClient-3.0'
            features.remove("wmqJmsClient-1.1");
            features.remove("wmqJmsClient-2.0");

            features.remove("heritageAPIs-1.0");
            features.remove("heritageAPIs-1.1");
        }

        // Test features may or may not be compatible,
        // we don't want to assert either way.
        features.removeAll(getTestFeatures());

        return features;
    }

    private static Set<String> getExtendedCompatibleFeatures(Set<String> compatibleFeatures,
                                                             boolean openLiberty) {

        Set<String> features = new HashSet<>(compatibleFeatures);

        // Remove features which cause conflicts.
        features.remove("jdbc-4.0");
        features.remove("jdbc-4.1");
        features.remove("sessionCache-1.0");
        features.remove("facesContainer-3.0");
        features.remove("jsonbContainer-2.0");
        features.remove("jsonpContainer-2.0");
        features.remove("passwordUtilities-1.1");
        features.remove("persistenceContainer-3.0");
        features.remove("mpOpenAPI-3.1");
        features.remove("mpTelemetry-1.1");

        // Remove client features.
        features.remove("jakartaeeClient-9.1");
        features.remove("appSecurityClient-1.0");

        // Remove noship features.
        features.remove("jcacheContainer-1.1");
        features.remove("netty-1.0");
        features.remove("noShip-1.0");
        features.remove("scim-2.0");

        // Remove acmeCA-2.0, which requires additional resources and configuration.
        features.remove("acmeCA-2.0");

        // Remove logAnalysis-1.0, which depends on hpel.
        features.remove("logAnalysis-1.0");

        features.remove("audit-2.0");

        // Conditionally remove features which have java dependencies:

        features.remove(JavaInfo.JAVA_VERSION < 9 ? "jdbc-4.3" : "jdbc-4.2");

        if (JavaInfo.JAVA_VERSION < 11) {
            features.remove("mpReactiveStreams-3.0");
            features.remove("mpReactiveMessaging-3.0");
            features.remove("mpTelemetry-1.1");
        }

        if (JavaInfo.JAVA_VERSION < 17) {
            features.remove("nosql-1.0");
        }

        return features;
    }

    private static Set<String> getIncompatibleFeatures(Set<String> versionedFeatures,
                                                       Set<String> compatibleFeatures,
                                                       boolean openLibertyOnly) {
        Set<String> features = new HashSet<>(versionedFeatures);

        features.removeAll(compatibleFeatures);

        // Test features may or may not be compatible, we don't want to assert either way
        features.removeAll(getTestFeatures());

        return features;
    }
}

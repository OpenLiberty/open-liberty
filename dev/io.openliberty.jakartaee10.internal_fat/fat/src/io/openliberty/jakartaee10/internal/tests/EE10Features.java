/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jakartaee10.internal.tests;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import componenttest.rules.repeater.EE7FeatureReplacementAction;
import componenttest.rules.repeater.EE8FeatureReplacementAction;
import componenttest.rules.repeater.FeatureUtilities;
import componenttest.rules.repeater.JakartaEE10Action;
import componenttest.rules.repeater.RepeatActions.EEVersion;
import componenttest.topology.impl.JavaInfo;

public class EE10Features {

    public static final boolean OPEN_LIBERTY_ONLY = true;

    public static Set<String> getInstalledFeatures(String installRoot, boolean openLibertyOnly) {
        return new HashSet<String>(FeatureUtilities.getFeaturesFromServer(new File(installRoot), openLibertyOnly));
    }

    public static Set<String> getVersionedFeatures(Set<String> features) {
        return FeatureUtilities.rejectVersionless(features);
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

    public static Set<String> getEE7Conflicts() {
        Set<String> features = new HashSet<>(EE7FeatureReplacementAction.EE7_FEATURE_SET);

        features.removeAll(getTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 with no replacement
        features.remove("j2eeManagement-1.1");

        // jcaInboundSecurity-1.0 was removed in Jakarta EE 10; replaced by an auto feature.
        features.remove("jcaInboundSecurity-1.0");

        // A couple of special cases that we want to make sure work.
        features.add("appSecurity-1.0");
        features.add("websocket-1.0");

        // servlet long name is the same for EE10.
        // it will fail because the prefixes match and it is
        // marked as a singleton.
        features.remove("servlet-3.1");

        return features;
    }

    public static Set<String> getEE8Conflicts() {
        Set<String> features = new HashSet<>(EE8FeatureReplacementAction.EE8_FEATURE_SET);

        features.removeAll(getTestFeatures());

        // j2eeManagement-1.1 was removed in Jakarta EE 9 with no replacement.
        features.remove("j2eeManagement-1.1");

        // jcaInboundSecurity-1.0 was removed in Jakarta EE 10, replace by an auto feature.
        features.remove("jcaInboundSecurity-1.0");

        // servlet long name is the same for EE10.
        // It will fail because the prefixes match and it is marked as a singleton.
        features.remove("servlet-4.0");

        return features;
    }

    //

    public EE10Features(String installRoot) throws Exception {
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

        // Non-ee10 features are not compatible
        features.removeAll(getEEFeatures(openLiberty));
        features.addAll(JakartaEE10Action.EE10_FEATURE_SET);

        // MP features are only compatible if they're in MP versions which work with EE10
        features.removeAll(getMPFeatures());
        features.addAll(getCompatibleMPFeatures(EEVersion.EE10));

        // Value-add features which aren't compatible
        features.remove("openid-2.0"); // stabilized
        features.remove("openapi-3.1"); // depends on mpOpenAPI
        features.remove("opentracing-1.0"); // opentracing depends on mpConfig
        features.remove("opentracing-1.1");
        features.remove("opentracing-1.2");
        features.remove("opentracing-1.3");
        features.remove("opentracing-2.0");
        features.remove("sipServlet-1.1"); // purposely not supporting EE 10
        features.remove("springBoot-1.5"); // springBoot 3.0 only supports EE10
        features.remove("springBoot-2.0");

        features.remove("mpHealth"); //versionless features in development
        features.remove("mpMetrics");

        if (!openLiberty) {
            // stabilized features
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

            // depend on previous EE versions and now uses wmqMessagingClient-3.0 for EE9
            features.remove("wmqJmsClient-1.1");
            features.remove("wmqJmsClient-2.0");

            // heritage API features
            features.remove("heritageAPIs-1.0");
            features.remove("heritageAPIs-1.1");
        }

        // Test features may or may not be compatible, we don't want to assert either way
        features.removeAll(getTestFeatures());

        return features;
    }

    private static Set<String> getExtendedCompatibleFeatures(Set<String> compatibleFeatures,
                                                             boolean openLiberty) {

        Set<String> features = new HashSet<>(compatibleFeatures);

        // remove features so that they don't cause feature conflicts.
        features.remove("jdbc-4.0");
        features.remove("jdbc-4.1");
        features.remove("jdbc-4.2");
        features.remove("sessionCache-1.0");
        features.remove("facesContainer-4.0");
        features.remove("jsonbContainer-3.0");
        features.remove("jsonpContainer-2.1");
        features.remove("passwordUtilities-1.1");
        features.remove("persistenceContainer-3.1");

        //remove MP 6.0 features which would conflict with MP 6.1 and 7.0 features
        features.remove("microProfile-6.0");
        features.remove("mpConfig-3.0");
        features.remove("mpTelemetry-1.0");
        features.remove("mpMetrics-5.0");
        features.remove("mpFaultTolerance-4.0");
        features.remove("mpRestClient-3.0");
        features.remove("mpOpenAPI-3.1");

        //remove MP 6.1 features which would conflict with MP 7.0 features
        features.remove("microProfile-6.1");
        features.remove("mpTelemetry-1.1");

        // remove client features
        features.remove("jakartaeeClient-10.0");
        features.remove("appSecurityClient-1.0");

        // remove acmeCA-2.0 since it requires additional resources and configuration
        features.remove("acmeCA-2.0");

        // remove noship features
        features.remove("jcacheContainer-1.1");
        features.remove("netty-1.0");
        features.remove("noShip-1.0");
        features.remove("scim-2.0");

        // remove logAnalysis-1.0.  It depends on hpel being configured
        features.remove("logAnalysis-1.0");

        features.remove("audit-2.0");

        // springBoot-3.0 and nosql-1.0 require Java 17 so if we are currently not using Java 17 or later, remove it from the list of features.
        if (JavaInfo.JAVA_VERSION < 17) {
            features.remove("springBoot-3.0");
            features.remove("nosql-1.0");
        }

        return features;
    }

    private Set<String> getIncompatibleFeatures(Set<String> versionedFeatures,
                                                Set<String> compatibleFeatures,
                                                boolean openLiberty) {
        Set<String> features = new HashSet<>(versionedFeatures);

        features.removeAll(compatibleFeatures);

        // Test features may or may not be compatible, we don't want to assert either way
        features.removeAll(getTestFeatures());

        return features;
    }
}

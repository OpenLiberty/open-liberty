/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.tests.util;

import static com.ibm.ws.feature.tests.util.FeatureNameCache.VERSION_COMPARATOR;
import static com.ibm.ws.feature.tests.util.FeatureNameCache.isAnomalous;
import static com.ibm.ws.feature.tests.util.FeatureUtil.getVersionLink;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isAuto;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isCompatibility;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isConvenience;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isPrivate;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isPublic;
import static com.ibm.ws.feature.tests.util.FeatureUtil.isVersionless;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.ws.kernel.feature.internal.subsystem.SubsystemFeatureDefinitionImpl;
import com.ibm.ws.kernel.feature.internal.util.BaseXML.FailableConsumer;
import com.ibm.ws.kernel.feature.provisioning.ProvisioningFeatureDefinition;

// @formatter:off

public class FeatureCategories {
    // Nomenclature:
    //    feature.symbolicName
    //    feature.featureName
    //
    //    feature.isAuto
    //    feature.isPublic | isProtected | isPrivate
    //
    //    feature.isConvenience (platform convenience feature)
    //    feature.isCompatibility (platform compatibility feature)
    //    feature.isVersionless
    //    feature.isVersionlessLink

    public static <T> Map<String, T> ensureBucket(Map<String, Map<String, T>> storage, String key) {
        Map<String, T> bucket = storage.get(key);
        if (bucket == null) {
            storage.put(key, bucket = new HashMap<>());
        }
        return bucket;
    }

    public final Map<String, ProvisioningFeatureDefinition> installedFeatures = new HashMap<>();

    public final Map<String, String> shortToSymbolic = new HashMap<>();
    public final Map<String, String> symbolicToShort = new HashMap<>();

    public final Map<String, String> akaToSymbolic = new HashMap<>();
    public final Map<String, String> symbolicToAKA = new HashMap<>();

    public final Map<String, ProvisioningFeatureDefinition> publicFeatures = new HashMap<>();
    public final Map<String, Map<String, ProvisioningFeatureDefinition>> cohorts = new HashMap<>();
    public final Map<String, ProvisioningFeatureDefinition> protectedFeatures = new HashMap<>();
    public final Map<String, ProvisioningFeatureDefinition> privateFeatures = new HashMap<>();

    public final Map<String, String> anomalousVersions = new HashMap<>();

    public String getVisibility(String symName) {
        if ( autoFeatures.containsKey(symName)) {
            return "auto";
        } else {
            return (publicFeatures.containsKey(symName) ? "public" :
                   (privateFeatures.containsKey(symName) ? "private" :
                   (protectedFeatures.containsKey(symName) ? "protected" : "unknown")));
        }
    }

    private final FeatureNameCache nameCache = new FeatureNameCache();

    private String[] parseName(String symName) {
        return nameCache.parseNameAndVersion(symName);
    }

    private String getVersion(String symName) {
        return nameCache.parseVersion(symName);
    }

    public final Map<String, ProvisioningFeatureDefinition> autoFeatures = new HashMap<>();

    public final Map<String, ProvisioningFeatureDefinition> convenienceFeatures = new HashMap<>();
    // public final Map<String, Map<String, ProvisioningFeatureDefinition>> convenienceCohorts = new HashMap<>();

    public final Map<String, ProvisioningFeatureDefinition> compatibilityFeatures = new HashMap<>();
    // public final Map<String, Map<String, ProvisioningFeatureDefinition>> compatibilityCohorts = new HashMap<>();

    public final Map<String, ProvisioningFeatureDefinition> versionlessFeatures = new HashMap<>();
    public final Map<String, ProvisioningFeatureDefinition> versionlessLinks = new HashMap<>();
    public final Map<String, Map<String, ProvisioningFeatureDefinition>> versionlessCohorts = new HashMap<>();

    public final Map<String, ProvisioningFeatureDefinition> versionedFeatures = new HashMap<>();
    public final Map<String, ProvisioningFeatureDefinition> nonVersionedFeatures = new HashMap<>();

    public String getType(String symName) {
        if ( convenienceFeatures.containsKey(symName) ) {
            return "convenience";
        } else if ( compatibilityFeatures.containsKey(symName) ) {
            return "compatibility";
        } else if ( versionlessFeatures.containsKey(symName) ) {
            return "versionless";
        } else if ( versionlessLinks.containsKey(symName) ) {
            return "versionless link";
        } else {
            return null;
        }
    }

    // Temporary: Requires an interface update to move to ProvisioningFeatureDefinition.
    public List<String> getAltNames(ProvisioningFeatureDefinition featureDef) {
        if ( !(featureDef instanceof SubsystemFeatureDefinitionImpl) ) {
            return Collections.emptyList();
        } else {
            return ((SubsystemFeatureDefinitionImpl) featureDef).getAltNames();
        }
    }

    public FeatureCategories(Collection<? extends ProvisioningFeatureDefinition> featureDefs) {
        for (ProvisioningFeatureDefinition featureDef : featureDefs) {
            String symName = featureDef.getSymbolicName();

            installedFeatures.put(symName, featureDef);

            if (isAuto(featureDef)) {
                autoFeatures.put(symName, featureDef);
                // Partition off auto-features; they are subject to less
                // stringent rules.
                continue;
            }

            boolean isPublic = isPublic(featureDef);
            boolean isPrivate = isPrivate(featureDef);
            if (isPublic) {
                publicFeatures.put(symName, featureDef);
            } else if (isPrivate) {
                privateFeatures.put(symName, featureDef);
            } else { // if ( isProtected (featureDef) ) {
                protectedFeatures.put(symName, featureDef);
            }

            String shortName = featureDef.getIbmShortName();
            if ((shortName != null) && !shortName.equals(symName)) {
                shortToSymbolic.put(shortName, symName);
                symbolicToShort.put(symName, shortName);
            }

            List<String> akaNames = getAltNames(featureDef);
            if ( akaNames.size() == 1 ) {
                String aka = akaNames.get(0);
                akaToSymbolic.put(aka, symName);
                symbolicToAKA.put(symName, aka);
            }

            String[] parts = parseName(symName);
            String baseName = parts[FeatureNameCache.NAME_OFFSET];
            String version = parts[FeatureNameCache.VERSION_OFFSET]; // May be null

            if (version != null) {
                ensureBucket(cohorts, baseName).put(version, featureDef);
                if (isAnomalous(version)) {
                    anomalousVersions.put(symName, version);
                }
            }

            String versionLink;
            if (isConvenience(featureDef)) {
                convenienceFeatures.put(symName, featureDef);
            } else if (isCompatibility(featureDef)) {
                compatibilityFeatures.put(symName, featureDef);
            } else if (isVersionless(featureDef)) {
                versionlessFeatures.put(symName, featureDef);
            } else if ((versionLink = getVersionLink(featureDef)) != null) {
                versionlessLinks.put(symName, featureDef);
                String linkVersion = getVersion(versionLink);
                ensureBucket(versionlessCohorts, baseName).put(linkVersion, featureDef);

            } else {
                if (!isPrivate) {
                    if ( version == null ) {
                        nonVersionedFeatures.put(symName, featureDef);
                    } else {
                        versionedFeatures.put(symName, featureDef);
                    }
                }
            }
        }
    }

    private static void println(PrintWriter writer, StringBuilder builder) {
        writer.println(builder.toString());
        builder.setLength(0);
    }

    private static void withBraces(StringBuilder builder, Object title, Object value) {
        builder.append(" ");
        builder.append(title);
        builder.append(" [ ");
        builder.append(value);
        builder.append(" ]");
    }

    private static void withBraces(StringBuilder builder, Object value) {
        builder.append(" [ ");
        builder.append(value);
        builder.append(" ]");
    }

    private static void dashLine(StringBuilder builder, char dashChar, int dashCount) {
        for ( int dashNo = 0; dashNo < dashCount; dashNo++ ) {
            builder.append(dashChar);
        }
    }

    private static void dashLine(PrintWriter writer, StringBuilder builder, char dashChar, int dashCount) {
        dashLine(builder, dashChar, dashCount);
        println(writer, builder);
    }

    private static void asList(StringBuilder builder, Collection<String> values) {
        boolean isFirst = true;
        for ( String value : values ) {
            if ( !isFirst ) {
                builder.append(", ");
            } else {
                isFirst = false;
            }
            builder.append(value);
        }
    }

    private static List<String> sortKeys(Map<String, ?> map) {
        List<String> keys = new ArrayList<>( map.keySet());
        Collections.sort(keys);
        return keys;
    }

    private static List<String> sortKeys(Map<String, ?> map, Comparator<String> cmp) {
        List<String> keys = new ArrayList<>( map.keySet());
        Collections.sort(keys, cmp);
        return keys;
    }

    public static final int LONG_DASH_LINE = 80;
    public static final int SHORT_DASH_LINE = 60;
    public static final char SINGLE_DASH = '-';
    public static final char DOUBLE_DASH = '=';

    public void report(PrintWriter writer) {
        StringBuilder lineBuilder = new StringBuilder();

        System.out.println("Features report:");
        dashLine(writer, lineBuilder, DOUBLE_DASH, LONG_DASH_LINE);

        writer.println("All features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> installedSymNames = sortKeys(installedFeatures);
        for ( String symName : installedSymNames ) {
            detailFeature(lineBuilder, symName);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Public features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> publicSymNames = sortKeys(publicFeatures);
        for ( String symName : publicSymNames ) {
            detailFeature(lineBuilder, symName);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Protected features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> protectedSymNames = sortKeys(protectedFeatures);
        for ( String symName : protectedSymNames ) {
            detailFeature(lineBuilder, symName);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Private features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> privateSymNames = sortKeys(privateFeatures);
        for ( String symName : privateSymNames ) {
            detailFeature(lineBuilder, symName);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Auto features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> autoSymNames = sortKeys(autoFeatures);
        for ( String symName : autoSymNames ) {
            writer.println(symName);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Feature cohort versions:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> baseNames = sortKeys(cohorts);
        for ( String baseName : baseNames ) {
            Map<String, ProvisioningFeatureDefinition> cohort = cohorts.get(baseName);
            lineBuilder.append(baseName);
            lineBuilder.append(": ");
            asList(lineBuilder, sortKeys(cohort, VERSION_COMPARATOR));
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Short names:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> symNames = sortKeys(symbolicToShort);
        for ( String symName : symNames ) {
            String shortName = symbolicToShort.get(symName);
            lineBuilder.append(symName);
            lineBuilder.append(": ");
            lineBuilder.append(shortName);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("AKA:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> akaSymNames = sortKeys(symbolicToAKA);
        for ( String symName : akaSymNames ) {
            String aka = symbolicToAKA.get(symName);
            lineBuilder.append(symName);
            lineBuilder.append(": ");
            lineBuilder.append(aka);
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Convenience features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> convenienceNames = sortKeys(convenienceFeatures);
        for ( String symName : convenienceNames ) {
            writer.println(symName);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Compatibility features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> compatibilityNames = sortKeys(compatibilityFeatures);
        for ( String symName : compatibilityNames ) {
            writer.println(symName);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Versionless feature versions:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> versionlessNames = sortKeys(versionlessCohorts);
        for ( String versionlessName : versionlessNames ) {
            Map<String, ProvisioningFeatureDefinition> versionlessCohort = versionlessCohorts.get(versionlessName);
            lineBuilder.append(versionlessName);
            lineBuilder.append(": ");
            asList(lineBuilder, sortKeys(versionlessCohort, VERSION_COMPARATOR));
            println(writer, lineBuilder);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        writer.println("Anomalous features:");
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);
        List<String> names = sortKeys(anomalousVersions);
        for ( String name : names ) {
            writer.println(name);
        }
        dashLine(writer, lineBuilder, SINGLE_DASH, SHORT_DASH_LINE);

        dashLine(writer, lineBuilder, DOUBLE_DASH, LONG_DASH_LINE);
    }

    public void detailFeature(StringBuilder lineBuilder, String symName) {
        lineBuilder.append(symName);
        String shortName = symbolicToShort.get(symName);
        if ( shortName != null ) {
            withBraces(lineBuilder, "as", shortName);
        }
        String aka = symbolicToAKA.get(symName);
        if ( aka != null ) {
            withBraces(lineBuilder, "aka", aka);
        }

        String featureType = getType(symName);
        if ( featureType != null ) {
            withBraces(lineBuilder, featureType);
        }

        String visibility = getVisibility(symName);
        withBraces(lineBuilder, visibility);
    }

    public static final boolean DO_APPEND = true;

    public static void write(File file, FailableConsumer<PrintWriter, Exception> writer) throws Exception {
        try (FileWriter fW = new FileWriter(file, !DO_APPEND);
             PrintWriter pW = new PrintWriter(fW)) {
            writer.accept(pW);
        }
    }

    public void write(File file) throws Exception {
        write(file, new FailableConsumer<PrintWriter, Exception>() {
            @Override
            public void accept(PrintWriter pW) throws Exception {
                try {
                    report(pW);
                } finally {
                    pW.flush();
                }
            }
        });
    }


    /*
     * Consolidated feature information for "appAuthentication / jaspic":

        Raw features:

        com.ibm.websphere.appserver.jaspic-1.1 as [ jaspic-1.1 ] [ public ]
        io.openliberty.appAuthentication-2.0 as [ appAuthentication-2.0 ] aka [ jaspic-2.0 ] [ public ]
        io.openliberty.appAuthentication-3.0 as [ appAuthentication-3.0 ] aka [ jaspic-3.0 ] [ public ]
        io.openliberty.appAuthentication-3.1 as [ appAuthentication-3.1 ] aka [ jaspic-3.1 ] [ public ]

        io.openliberty.versionless.jaspic as [ jaspic ] [ versionless ] [ public ]
        io.openliberty.versionless.appAuthentication as [ appAuthentication ] [ versionless ] [ public ]

        io.openliberty.internal.versionless.jaspic-1.1 [ versionless link ] [ private ]
        io.openliberty.internal.versionless.jaspic-2.0 [ versionless link ] [ private ]
        io.openliberty.internal.versionless.jaspic-3.0 [ versionless link ] [ private ]

        Short names:

        com.ibm.websphere.appserver.jaspic-1.1: jaspic-1.1
        io.openliberty.appAuthentication-2.0: appAuthentication-2.0
        io.openliberty.appAuthentication-3.0: appAuthentication-3.0
        io.openliberty.appAuthentication-3.1: appAuthentication-3.1

        AKA:

        io.openliberty.appAuthentication-2.0: jaspic-2.0
        io.openliberty.appAuthentication-3.0: jaspic-3.0
        io.openliberty.appAuthentication-3.1: jaspic-3.1

        Short versionless:

        io.openliberty.versionless.jaspic: jaspic
        io.openliberty.versionless.appAuthentication: appAuthentication

        Feature ranges:

        com.ibm.websphere.appserver.jaspic: 1.1
        io.openliberty.appAuthentication: 2.0, 3.0, 3.1
        io.openliberty.internal.versionless.jaspic: 1.1, 2.0, 3.0

    * Merged feature range:

        jaspic-1.1: com.ibm.websphere.appserver.jaspic-1.1
        jaspic-2.0: io.openliberty.appAuthentication-2.0
        jaspic-3.0: io.openliberty.appAuthentication-3.0
        jaspic-4.0: io.openliberty.appAuthentication-3.1

    */

    // @formatter:on
}

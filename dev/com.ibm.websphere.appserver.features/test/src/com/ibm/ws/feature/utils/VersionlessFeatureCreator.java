/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.feature.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionlessFeatureCreator {

    private Map<String, FeatureInfo> _featureRepo;

    private final String privatePath = "build/versionless/visibility/private/";
    private final String publicPath = "build/versionless/visibility/public/";

    private final String checkExistingPublic = "visibility/public/";
    private final String checkExistingPrivate = "visibility/private/";

    private final boolean createAll = false;

    public VersionlessFeatureCreator(Map<String, FeatureInfo> featureRepo){
        this._featureRepo = featureRepo;
    }

    public boolean createFeatureFiles(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) throws IOException {
        File priv = new File(privatePath);
        if (!priv.exists()) {
            priv.mkdirs();
        }
        File pub = new File(publicPath);
        if (!pub.exists()) {
            pub.mkdirs();
        }

        if (akaFeature != null) {
            // this feature is the older version of an newer feature
            // ex. ejb
            // in this scenario we add the future feature versions
            ArrayList<String[]> temp = akaFeature.getFeaturesAndPlatformAndKind();
            for (String[] featAndPlat : temp) {
                feature.addFeaturePlatformAndKind(featAndPlat);
            }
        } else {
            if (feature.getAKAFutureFeature() != null) {
                feature.setAKAFutureFeature(null);
            }
            if (feature.getAlsoKnownAs() != null) {
                feature.setAlsoKnownAs(null);
            }
        }

        boolean generatedNewFile = false;

        //features array:
        //  features[0] == the name of the feature ex. servlet-4.0
        //  features[1] == the name of the platform it depends on ex. jakartaPlatform-8.0
        //  features[2] == the full name of the feature ex. com.ibm.ws.servlet-4.0
        if (feature.getAlsoKnownAs() == null) {
            for (String[] features : feature.getFeaturesAndPlatformAndKind()) {
                
                //Code for utilizing the ee/mp versions to add within the private feature defs
                String[] dependencyVersions = feature.getAllDependencyVersions(features[0], features[1].split("-")[0]);

                String x = null;
                String y = null;
                if (feature.getFeatureName().startsWith("mp")) {
                    x = "io.openliberty.internal.mpVersion";
                    if (dependencyVersions[1].equals("")) {
                        y = features[1].split("-")[1];
                    } else {
                        y = dependencyVersions[0] + "; ibm.tolerates:=\"" + dependencyVersions[1] + "\"";
                    }
                }

                if (createPrivateVersionedFeature(feature.getFeatureName(), akaFeature == null ? null : akaFeature.getFeatureName(), features[0].split("-")[1], x, y,
                                                  features[2], feature.getEdition(), features[3])) {
                    generatedNewFile = true;
                }
            }
        }

        if (createPublicVersionlessFeature(feature, akaFeature)) {
            generatedNewFile = true;
        }
        createPublicFeaturePropertiesFile(feature, akaFeature);

        return generatedNewFile;
    }

    private boolean createPrivateVersionedFeature(String featureName, String akaFeatureName, String featureNum, String mpVersionBaseName, String mpVersionTolerates, String fullName,
                                                  String edition, String kind) throws IOException {
        File checkExisting = new File(checkExistingPrivate + "io.openliberty.internal.versionless." + featureName + "-" + featureNum + ".feature");
        if (createAll) {
            //skip checking, just create the feature
        } else if (checkExisting.exists() && validatePrivateLinkingFeature(featureName, featureNum, mpVersionBaseName, mpVersionTolerates, fullName)) {
            return false;
        } else if (akaFeatureName != null) {
            checkExisting = new File(checkExistingPrivate + "io.openliberty.internal.versionless." + akaFeatureName + "-" + featureNum + ".feature");
            if (checkExisting.exists() && validatePrivateLinkingFeature(akaFeatureName, featureNum, mpVersionBaseName, mpVersionTolerates, fullName)) {
                return false;
            }
        }
        File f = new File(privatePath + "io.openliberty.internal.versionless." + featureName + "-" + featureNum + ".feature");
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
        writer.newLine();
        writer.append("symbolicName=io.openliberty.internal.versionless." + featureName + "-" + featureNum);
        writer.newLine();
        writer.append("visibility=private");
        writer.newLine();
        writer.append("singleton=true");
        writer.newLine();
        writer.append("-features= \\");
        writer.newLine();
        if ("noship".equals(kind)) {
            writer.append("    io.openliberty.noShip-1.0, \\");
            writer.newLine();
        }
        if (mpVersionBaseName != null && mpVersionTolerates != null) {
            writer.append("    " + mpVersionBaseName + "-" + mpVersionTolerates + ", \\");
            writer.newLine();
        }
        writer.append("    " + fullName);
        writer.newLine();
        writer.append("kind=" + kind);
        writer.newLine();
        writer.append("edition=" + ("noship".equals(kind) ? "full" : edition));
        writer.newLine();

        writer.close();

        return true;
    }

    private boolean createPublicVersionlessFeature(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) throws IOException {
        ArrayList<String> existingVersions = null;
        File checkExisting = new File(checkExistingPublic + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        //Even if we already have an existing public versionless feature,
        //if we created a new private versionless feature we need to update the public feature with new dependencies
        if (checkExisting.exists()) {
            existingVersions = validatePublicVersionlessFeature(feature, akaFeature);
            if (existingVersions == null && !createAll) {
                return false;
            }
        }

        File dir = new File(publicPath + feature.getFeatureName());
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File f = new File(publicPath + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
        writer.newLine();
        writer.append("symbolicName=io.openliberty.versionless." + feature.getFeatureName());
        writer.newLine();
        writer.append("visibility=public");
        writer.newLine();
        writer.append("IBM-ShortName: " + feature.getFeatureName());
        writer.newLine();
        writer.append("Subsystem-Name: " + feature.getSubsystemName());
        writer.newLine();
        String toleratesFeature = feature.getFeatureName();
        if (feature.getAlsoKnownAs() != null) {
            toleratesFeature = feature.getAlsoKnownAs();
        }
        String[] versions = null;
        if (existingVersions != null) {
            ArrayList<String> allVersions = new ArrayList<>();
            for (String s : feature.getAllVersions()) {
                allVersions.add(s);
                if (existingVersions.contains(s)) {
                    existingVersions.remove(s);
                }
            }
            allVersions.addAll(existingVersions);
            versions = feature.getPreferredAndTolerates(allVersions);
        } else {
            versions = feature.getPreferredAndTolerates();
        }

        if (versions.length == 1) {
            writer.append("-features=io.openliberty.internal.versionless." + toleratesFeature + "-" + versions[0]);
        } else {
            writer.append("-features=io.openliberty.internal.versionless." + toleratesFeature + "-" + versions[0] + "; ibm.tolerates:=\"" + versions[1] + "\"");
        }
        writer.newLine();
        writer.append("kind=" + feature.getKind());
        writer.newLine();
        writer.append("edition=" + feature.getEdition());
        writer.newLine();
        writer.append("WLP-InstantOn-Enabled: true");
        writer.newLine();

        writer.close();

        return true;
    }

    private void createPublicFeaturePropertiesFile(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) throws IOException {
        File checkExisting = new File(checkExistingPublic + feature.getFeatureName() + "/resources/l10n/io.openliberty.versionless." + feature.getFeatureName() + ".properties");
        if (checkExisting.exists()) {
            return;
        }
        File dir = new File(publicPath + feature.getFeatureName() + "/resources/l10n");
        dir.mkdirs();
        File f = new File(publicPath + feature.getFeatureName() + "/resources/l10n/io.openliberty.versionless." + feature.getFeatureName() + ".properties");
        if (f.exists()) {
            f.delete();
        }
        f.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(f));
        writer.append("###############################################################################");
        writer.newLine();
        writer.append("# Copyright (c) 2024 IBM Corporation and others.");
        writer.newLine();
        writer.append("# All rights reserved. This program and the accompanying materials");
        writer.newLine();
        writer.append("# are made available under the terms of the Eclipse Public License 2.0");
        writer.newLine();
        writer.append("# which accompanies this distribution, and is available at");
        writer.newLine();
        writer.append("# http://www.eclipse.org/legal/epl-2.0/");
        writer.newLine();
        writer.append("# ");
        writer.newLine();
        writer.append("# SPDX-License-Identifier: EPL-2.0");
        writer.newLine();
        writer.append("###############################################################################");
        writer.newLine();
        writer.append("#");
        writer.newLine();
        writer.append("#ISMESSAGEFILE FALSE");
        writer.newLine();
        writer.append("#NLS_ENCODING=UNICODE");
        writer.newLine();
        writer.append("#NLS_MESSAGEFORMAT_NONE");
        writer.newLine();
        writer.append("#");
        writer.newLine();
        writer.newLine();
        writer.append("description=This feature enables support for versionless " + feature.getFeatureName());

        writer.newLine();

        writer.close();
    }

    /*
     * True if existing public versionless feature has correct dependencies
     * false if not
     */
    private ArrayList<String> validatePublicVersionlessFeature(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) {
        File existingFeature = new File(checkExistingPublic + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        ArrayList<String> existingFeatureVersions = new ArrayList<String>();
        String featureFullName = "io.openliberty.internal.versionless." + feature.getFeatureName() + "-";
        String akaFeatureFullName = akaFeature == null ? null : "io.openliberty.internal.versionless." + akaFeature.getFeatureName() + "-";
        try {
            Scanner myReader = new Scanner(existingFeature);
            while (myReader.hasNextLine()) {
                String s = myReader.nextLine();
                String privateFeatureFullName = null;
                if (s.contains(featureFullName)) {
                    privateFeatureFullName = featureFullName;
                } else if (akaFeatureFullName != null && s.contains(akaFeatureFullName)) {
                    privateFeatureFullName = akaFeatureFullName;
                }
                if (privateFeatureFullName != null) {
                    //check version after dash
                    if (s.contains("ibm.tolerates")) {
                        existingFeatureVersions.add(s.substring(s.indexOf(privateFeatureFullName) + privateFeatureFullName.length(), s.indexOf(";")));
                        Pattern p = Pattern.compile("\"(.*?)\"");
                        Matcher m = p.matcher(s);
                        while (m.find()) {
                            existingFeatureVersions.addAll(Arrays.asList(m.group(1).split(",")));
                        }

                    } else {
                        //if there is no ibm.tolerates, this is the end of the line.
                        String version = s.substring(s.indexOf(privateFeatureFullName) + privateFeatureFullName.length()).trim();
                        existingFeatureVersions.add(version.substring(0, version.indexOf(".") + 2));
                    }
                    break;
                }
            }
            myReader.close();
        } catch (Exception e) {
            // We check if file exists before entering this function, should never reach this
        }

        if (createAll) {
            return existingFeatureVersions;
        }

        ArrayList<String> featureVersions = feature.getAllVersions();

        for (String v : existingFeatureVersions) {
            if (featureVersions.contains(v)) {
                featureVersions.remove(v);
            }
        }
        if (!!!featureVersions.isEmpty()) {
            return existingFeatureVersions;
        }

        return null;
    }

    /*
     * True if existing private linking feature has correct dependencies
     * false if not
     */
    private boolean validatePrivateLinkingFeature(String featureName, String featureVersion, String mpVersionBase, String mpVersionTolerates, String versionedFeatureName) {
        FeatureInfo feature = _featureRepo.get("io.openliberty.internal.versionless." + featureName + "-" + featureVersion);
        if(feature == null){
            return false;
        }

        File featureFile = feature.getFeatureFile();
        boolean checkForMpVersion = (mpVersionBase != null);
        boolean containsVersionedFeature = false;
        boolean containsMpVersion = false;
        try {
            Scanner myReader = new Scanner(featureFile);
            while (myReader.hasNextLine()) {
                String s = myReader.nextLine();
    
                if(checkForMpVersion && s.trim().equals(mpVersionTolerates == null ? mpVersionBase : (mpVersionBase + "-" + mpVersionTolerates) + ", \\")){
                    containsMpVersion = true;
                }
                else if(s.trim().equals(versionedFeatureName)){
                    containsVersionedFeature = true;
                }
            }
            myReader.close();
        }
        catch (Exception e) {
            // We check if file exists before entering this function, should never reach this
        }

        if(checkForMpVersion && !containsMpVersion){
            return false;
        }
        if(!containsVersionedFeature){
            return false;
        }

        return true;
    }
}

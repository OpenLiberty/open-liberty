package com.ibm.ws.feature.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionlessFeatureCreator {

    private String privatePath = "build/versionless/visibility/private/";
    private String publicPath = "build/versionless/visibility/public/";

    private String checkExistingPublic = "visibility/public/";
    private String checkExistingPrivate = "visibility/private/";

    public boolean createFeatureFiles(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) throws IOException {
        File priv = new File(privatePath);
    	if(!priv.exists()){
            priv.mkdirs();
        }
        File pub = new File(publicPath);
    	if(!pub.exists()){
            pub.mkdirs();
        }

        if(akaFeature != null){
            // this feature is the older version of an newer feature
            // ex. ejb
            // in this scenario we add the future feature versions
            ArrayList<String[]> temp = akaFeature.getFeaturesAndPlatform();
            for(String[] featAndPlat : temp){
                feature.addFeaturePlatform(featAndPlat);
            }
        }
        else{
            if(feature.getAKAFutureFeature() != null){
                feature.setAKAFutureFeature(null);
            }
            if(feature.getAlsoKnownAs() != null){
                feature.setAlsoKnownAs(null);
            }
        }

        boolean generatedNewFile = false;


        //features array:
        //  features[0] == the name of the feature ex. servlet-4.0
        //  features[1] == the name of the platform it depends on ex. jakartaPlatform-8.0
        //  features[2] == the full name of the feature ex. com.ibm.ws.servlet-4.0
        if(feature.getAlsoKnownAs() == null){
            for(String[] features : feature.getFeaturesAndPlatform()) {
                //Code for utilizing the ee/mp versions to add within the private feature defs
                String[] dependencyVersions = feature.getAllDependencyVersions(features[0], features[1].split("-")[0]);
                
                String x = null;
                String y = null;
                if(feature.getFeatureName().startsWith("mp")){
                    x = "io.openliberty.internal.versionlessMP";
                    if(dependencyVersions[1].equals("")){
                        y = features[1].split("-")[1];
                    }
                    else{
                        y = dependencyVersions[0]+"; ibm.tolerates:=\"" + dependencyVersions[1] + "\"";
                    }
                }

                if(createPrivateVersionedFeature(feature.getFeatureName(), features[0].split("-")[1], x, y, features[2])){
                    generatedNewFile = true;
                }
            }
        }

        if(createPublicVersionlessFeature(feature)){
            generatedNewFile = true;
        };
        createPublicFeaturePropertiesFile(feature);

        return generatedNewFile;
    }

    private boolean createPrivateVersionedFeature(String featureName, String featureNum, String x, String y, String fullName) throws IOException {
        // File checkExisting = new File(checkExistingPrivate + "io.openliberty.internal.versionless." + featureName + "-" + featureNum + ".feature");
        // if(checkExisting.exists()){
        //     return false;
        // }
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
        if(x != null && y != null){
            writer.append("    " + x + "-" + y + ", \\");
            writer.newLine();
        }
        writer.append("    " + fullName);
        writer.newLine();
        writer.append("kind=beta");
        writer.newLine();
        writer.append("edition=base");
        writer.newLine();
        
        writer.close();
        
        return true;
    }

    private boolean createPublicVersionlessFeature(VersionlessFeatureDefinition feature) throws IOException {
        // File checkExisting = new File(checkExistingPublic + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        // //Even if we already have an existing public versionless feature, 
        // //if we created a new private versionless feature we need to update the public feature with new dependencies
        // if(checkExisting.exists() && validatePublicVersionlessFeature(feature)){
        //     return false;
        // }
    	File dir = new File(publicPath + feature.getFeatureName());
    	if(!dir.exists()) {
    		dir.mkdirs();
        }
        File f = new File(publicPath + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        if(f.exists()){
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
        String[] versions = feature.getPreferredAndTolerates();
        String toleratesFeature = feature.getFeatureName();
        if(feature.getAlsoKnownAs() != null){
            toleratesFeature = feature.getAlsoKnownAs();
        }
        if(versions.length == 1){
            writer.append("-features=io.openliberty.internal.versionless." + toleratesFeature + "-" + versions[0]);
        }
        else{
            writer.append("-features=io.openliberty.internal.versionless." + toleratesFeature + "-" + versions[0] + "; ibm.tolerates:=\"" + versions[1] + "\"");
        }
        writer.newLine();
        writer.append("kind=beta");
        writer.newLine();
        writer.append("edition=base");
        writer.newLine();

        writer.close();

        return true;
    }

    private void createPublicFeaturePropertiesFile(VersionlessFeatureDefinition feature) throws IOException {
        // File checkExisting = new File(checkExistingPublic + feature.getFeatureName() + "/resources/l10n/io.openliberty.versionless." + feature.getFeatureName() + ".properties");
        // if(checkExisting.exists()){
        //     return;
        // }
    	File dir = new File(publicPath + feature.getFeatureName() + "/resources/l10n");
        dir.mkdirs();
        File f = new File(publicPath + feature.getFeatureName() + "/resources/l10n/io.openliberty.versionless." + feature.getFeatureName() + ".properties");
        if(f.exists()){
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
    private boolean validatePublicVersionlessFeature(VersionlessFeatureDefinition feature) {
        File existingFeature = new File(checkExistingPublic + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
        ArrayList<String> existingFeatureVersions = new ArrayList<String>();
        String featureFullName = "io.openliberty.internal.versionless." + feature.getFeatureName() + "-";
        if(feature.getAlsoKnownAs() != null){
            featureFullName = "io.openliberty.internal.versionless." + feature.getAlsoKnownAs() + "-";
        }
        try {
            Scanner myReader = new Scanner(existingFeature);
            while (myReader.hasNextLine()) {
                String s = myReader.nextLine();
                if(s.contains(featureFullName)){
                    //check version after dash
                    if(s.contains("ibm.tolerates")){
                        existingFeatureVersions.add(s.substring(s.indexOf(featureFullName) + featureFullName.length(), s.indexOf(";")));
                        Pattern p = Pattern.compile("\"(.*?)\"");
                        Matcher m = p.matcher(s);
                        while(m.find())
                        {
                            existingFeatureVersions.addAll(Arrays.asList(m.group(1).split(",")));
                        }
                        
                    }
                    else{
                        //if there is no ibm.tolerates, this is the end of the line.
                        String version = s.substring(s.indexOf(featureFullName) + featureFullName.length()).trim();
                        existingFeatureVersions.add(version.substring(0, version.indexOf(".") + 2));
                    }
                    break;
                }
            }
            myReader.close();
        }
        catch(Exception e) {
            // We check if file exists before entering this function, should never reach this
        }

        ArrayList<String> featureVersions = feature.getAllVersions();
        ArrayList<String> copyExistingVersions = (ArrayList<String>) existingFeatureVersions.clone();

        for(String v : existingFeatureVersions){
            if(featureVersions.contains(v)){
                copyExistingVersions.remove(v);
                featureVersions.remove(v);
            }
        }
        if(!!!featureVersions.isEmpty() || !!!copyExistingVersions.isEmpty()){
            return false;
        }

        return true;
    }
}
package com.ibm.ws.feature.utils;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.FileReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import java.util.ArrayList;

public class VersionlessFeatureCreator {

    private String privatePath = "visibility/private/";
    private String publicPath = "visibility/public/";

    public void createFeatureFiles(VersionlessFeatureDefinition feature, VersionlessFeatureDefinition akaFeature) throws IOException {
        File priv = new File(privatePath);
    	if(!priv.exists()){
            priv.mkdirs();
        }
        File pub = new File(publicPath);
    	if(!pub.exists()){
            pub.mkdirs();
        }

        if(akaFeature != null){
            if(feature.getAKAFutureFeature() != null){
                System.out.println("AKA FUTURE FEATURE -------------" + feature.getFeatureName() + "------------");
                // this feature is the older version of an newer feature
                // ex. ejb
                // in this scenario we add the future feature versions
                ArrayList<String[]> temp = akaFeature.getFeaturesAndPlatform();
                for(String[] featAndPlat : temp){
                    feature.addFeaturePlatform(featAndPlat);
                }
            }
        }

        createPublicVersionlessFeature(feature);
        createPublicFeaturePropertiesFile(feature);

        //features array:
        //  features[0] == the name of the feature ex. servlet-4.0
        //  features[1] == the name of the platform it depends on ex. jakartaPlatform-8.0
        //  features[2] == the full name of the feature ex. com.ibm.ws.servlet-4.0
    	for(String[] features : feature.getFeaturesAndPlatform()) {
            // String[] dependencyVersions = feature.getAllDependencyVersions(features[0], features[1].split("-")[0]);
            // if(dependencyVersions[1].equals("")){
            //     createPrivateVersionedFeature(feature.getFeatureName(), features[0].split("-")[1], features[1].split("-")[0], features[1].split("-")[1], features[2]);
            // }
            // else{
            //     createPrivateVersionedFeature(feature.getFeatureName(), features[0].split("-")[1], features[1].split("-")[0], dependencyVersions[0]+"; ibm.tolerates:=\"" + dependencyVersions[1] + "\"", features[2]);
            // }
            
            if(feature.getAKAFutureFeature() != null){
                if(!feature.getFeatureName().equals(features[0].split("-")[0])){
                    continue;
                }
            }
            if(feature.getAlsoKnownAs() != null){
                System.out.println("AKA PAST FEATURE -------------" + feature.getFeatureName() + "------------");
            }
            createPrivateVersionedFeature(feature.getFeatureName(), features[0].split("-")[1], features[2], feature.getAlsoKnownAs());
        }
    }
    
    private void createPrivateVersionedFeature(String featureName, String featureNum, String fullName, String aka) throws IOException {
    	File f = new File(privatePath + "io.openliberty.internal.versionless." + featureName + "-" + featureNum + ".feature");
    	if(!f.exists()) {
    		f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
            writer.newLine();
            writer.append("symbolicName=io.openliberty.internal.versionless." + featureName + "-" + featureNum);
            writer.newLine();
            if(aka != null){
                writer.append("WLP-AlsoKnownAs: io.openliberty.internal.versionless." + aka + "-" + featureNum);
                writer.newLine();
            }
            writer.append("visibility=private");
            writer.newLine();
            writer.append("singleton=true");
            writer.newLine();
            writer.append("-features= \\");
            writer.newLine();
            writer.append("    " + fullName);
            writer.newLine();
            writer.append("kind=noship");
            writer.newLine();
            writer.append("edition=full");
            writer.newLine();
            
            writer.close();
    	}
    }
    
    private void createPublicVersionlessFeature(VersionlessFeatureDefinition feature) throws IOException {
    	File dir = new File(publicPath + feature.getFeatureName());
    	if(!dir.exists()) {
    		dir.mkdirs();
        	File f = new File(publicPath + feature.getFeatureName() + "/io.openliberty.versionless." + feature.getFeatureName() + ".feature");
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
            String[] versions = feature.getAllVersions();
            if(versions.length == 1){
                writer.append("-features=io.openliberty.internal.versionless." + feature.getFeatureName() + "-" + versions[0]);
            }
            else{
                writer.append("-features=io.openliberty.internal.versionless." + feature.getFeatureName() + "-" + versions[0] + "; ibm.tolerates:=\"" + versions[1] + "\"");
            }
            writer.newLine();
            writer.append("kind=noship");
            writer.newLine();
            writer.append("edition=full");
            writer.newLine();
            
            writer.close();
    	}
    }
    
    private void createPublicFeaturePropertiesFile(VersionlessFeatureDefinition feature) throws IOException {
    	File dir = new File(publicPath + feature.getFeatureName() + "/resources/l10n");
        if(!dir.exists()){
            dir.mkdirs();
            File f = new File(publicPath + feature.getFeatureName() + "/resources/l10n/io.openliberty." + feature.getFeatureName() + ".properties");
            f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.append("###############################################################################");
            writer.newLine();
            writer.append("# Copyright (c) 2023 IBM Corporation and others.");
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
	}
}

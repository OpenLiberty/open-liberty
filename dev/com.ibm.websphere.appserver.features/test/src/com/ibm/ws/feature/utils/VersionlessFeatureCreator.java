package com.ibm.ws.feature.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class VersionlessFeatureCreator {

    private String privatePath = "temp/visibility/private/";
    private String publicPath = "temp/visibility/public/";

    /** Private/Internal versioned/versionless Jakarta
    
    -include= ~${workspace}/cnf/resources/bnd/feature.props
    symbolicName=io.openliberty.jakartaPlatform.internal-9.1
    singleton=true
    kind=noship
    edition=full
    WLP-Activation-Type: parallel

    */

    /** Public versioned Jakarta
    
    -include= ~${workspace}/cnf/resources/bnd/feature.props
    symbolicName=io.openliberty.jakartaPlatform-9.1
    visibility=public
    singleton=true
    IBM-App-ForceRestart: install, uninstall
    IBM-ShortName: jakartaPlatform-9.1
    Subsystem-Name: Jakarta Platform 9.1
    -features=io.openliberty.jakartaPlatform.internal-9.1
    kind=noship
    edition=full

    */


    // I think we can take out the 'com.ibm.websphere.appserver.transaction-1.2; ibm.tolerates:="2.0", \' type features from
    // the public versionless features as they are brought in by the already existing versioned public features
    // So it would just be '-features=io.openliberty.unversioned.persistence-0.0; ibm.tolerates:="2.0,2.1,2.2,3.0,3.1,3.2"'
    public void createPrivateFeatures(VersionlessFeatureDefinition feature) throws IOException {
        File f = new File(privatePath);
    	if(!f.exists()){
            f.mkdirs();
        }

    	createPrivateVersionlessFeature(feature.getFeatureName(), "jakartaPlatform");
    	for(String[] features : feature.getFeaturesAndPlatform()) {
    		createPrivateVersionedFeature(feature.getFeatureName(), features[0].split("-")[1], features[1].split("-")[0], features[1].split("-")[1], features[2]);
    	}
    }
    
    private void createPrivateVersionedFeature(String featureName, String featureNum, String dependsOnName, String dependsOnNum, String fullName) throws IOException {
    	File f = new File(privatePath + "io.openliberty.unversioned." + featureName + "-" + featureNum + ".feature");
    	if(!f.exists()) {
    		f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
            writer.newLine();
            writer.append("symbolicName=io.openliberty.unversioned." + featureName + "-" + featureNum);
            writer.newLine();
            writer.append("visibility=private");
            writer.newLine();
            writer.append("singleton=true");
            writer.newLine();
            writer.append("-features= \\");
            writer.newLine();
            writer.append("    io.openliberty." + dependsOnName + ".internal-" + dependsOnNum + ", \\");
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
    
    public void createPublicVersionlessFeature(VersionlessFeatureDefinition feature) throws IOException {
    	File dir = new File(publicPath + feature.getFeatureName());
    	if(!dir.exists()) {
    		dir.mkdirs();
        	File f = new File(publicPath + feature.getFeatureName() + "/io.openliberty." + feature.getFeatureName() + ".feature");
    		f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
            writer.newLine();
            writer.append("symbolicName=io.openliberty." + feature.getFeatureName());
            writer.newLine();
            writer.append("visibility=public");
            writer.newLine();
            writer.append("IBM-ShortName: " + feature.getFeatureName());
            writer.newLine();
            writer.append("Subsystem-Name: " + feature.getSubsystemName());
            writer.newLine();
            writer.append("-features=io.openliberty.unversioned." + feature.getFeatureName() + "-0.0; ibm.tolerates:=\"" + feature.getAllVersions() + "\"");
            writer.newLine();
            writer.append("WLP-Required-Feature: jakartaPlatform, javaeePlatform, mpPlatform");
            writer.newLine();
            writer.append("kind=noship");
            writer.newLine();
            writer.append("edition=full");
            writer.newLine();
            
            writer.close();
    	}
    	
    	createPublicFeaturePropertiesFile(feature);
    }
    
    private void createPublicFeaturePropertiesFile(VersionlessFeatureDefinition feature) throws IOException {
    	File dir = new File(publicPath + feature.getFeatureName() + "/resources/l10n");
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
    
    private void createPrivateVersionlessFeature(String featureName, String dependsOnName) throws IOException {
    	File f = new File(privatePath + "io.openliberty.unversioned." + featureName + "-0.0.feature");
    	if(!f.exists()) {
    		f.createNewFile();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.append("-include= ~${workspace}/cnf/resources/bnd/feature.props");
            writer.newLine();
            writer.append("symbolicName=io.openliberty.unversioned." + featureName + "-0.0");
            writer.newLine();
            writer.append("visibility=private");
            writer.newLine();
            writer.append("singleton=true");
            writer.newLine();
            writer.append("-features=io.openliberty." + dependsOnName + ".internal-0.0");
            writer.newLine();
            writer.append("kind=noship");
            writer.newLine();
            writer.append("edition=full");
            writer.newLine();
            
            writer.close();
    	}
    }
}

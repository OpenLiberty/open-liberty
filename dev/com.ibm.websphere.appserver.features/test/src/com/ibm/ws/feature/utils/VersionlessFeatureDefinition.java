/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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

 package com.ibm.ws.feature.utils;

import java.util.ArrayList;
import java.util.Collections;

public class VersionlessFeatureDefinition {
	
	private String featureName;
	private String subsystemName;
	private ArrayList<String[]> featuresAndPlatform;
	
	public VersionlessFeatureDefinition(String featureName, String subsystemName, ArrayList<String[]> featuresAndPlatform) {
		this.featureName = featureName;
		this.subsystemName = subsystemName;
		this.featuresAndPlatform = featuresAndPlatform;
	}

	public VersionlessFeatureDefinition(String featureName, String subsystemName, String[] featureAndPlatform) {
		this.featureName = featureName;
		this.subsystemName = subsystemName;
		this.featuresAndPlatform = new ArrayList<String[]>();
        featuresAndPlatform.add(featureAndPlatform);
	}

    /**
     * Get the Feature Name for this feature.
     * 
     * @return
     */
    public String getFeatureName() {
    	return this.featureName;
    }
    
    /**
     * Get the Feature Name for this feature.
     * 
     * @return
     */
    public String getSubsystemName() {
    	return this.subsystemName;
    }
    
    /**
     * Get the features mapped to their platform dependency
     * EX:
     * jpa-2.2, jakartaPlatform-8.0
     * persistence-3.0, JakartaPlatform-9.1
     *  
     * @return
     */
    public ArrayList<String[]> getFeaturesAndPlatform() {
    	return this.featuresAndPlatform;
    }

    public void addFeaturePlatform(String[] featurePlatform) {
        featuresAndPlatform.add(featurePlatform);
    }

    public void addFeaturePlatform(String feature, String platform) {
        featuresAndPlatform.add(new String[] { feature, platform });
    }
    
    /**
     * Gets all versions of this feature so the 'ibm.tolerates:="2.0,2.1,2.2,3.0,3.1,3.2"' can be created
     * @return
     */
    public String getAllVersions() {
    	ArrayList<String> versions = new ArrayList<String>();
        String output = "";

    	for(String[] featAndPlat : featuresAndPlatform) {
            if(!versions.contains(featAndPlat[0].split("-")[1])){
                versions.add(featAndPlat[0].split("-")[1]);
            }
    	}
        Collections.sort(versions);
        for(String version : versions){
            output += version + ",";
        }
    	
    	output = output.substring(0, output.length()-1);
    	
    	return output;
    }
    
    /**
     * Gets every version of a dependency that a versioned feature uses.
     * Ex. Servlet-4.0 can be used on mp1.0, mp-1.1, mp1.2, mp1.3 and so on. 
     * This gets the lowest version of the dependency at index 0. For the above example it would be "1.0"
     * This gathers all versions, except the lowest, of the dependency at index 1. 
     * For the above example it would be "1.1,1.2,1.3"
     * @return
     */
    public String[] getAllDependencyVersions(String versionedFeature, String dependency) {
        String result = "";
        String currentLow = "" + Double.MAX_VALUE;

        for(int i = 0; i < featuresAndPlatform.size(); i++){
            String[] fnp = featuresAndPlatform.get(i);
            if(fnp[0].equals(versionedFeature)){
                if(fnp[1].contains(dependency + "-")){
                    //Should leave you with "(version number).feature" ex. "1.2"
                    String temp = fnp[1].split("-")[1];
                    int compare = compareVersions(currentLow, temp);
                    if(compare == 1){
                        if(!currentLow.equals("" + Double.MAX_VALUE)){
                            result += currentLow + ",";
                        }
                        currentLow = temp;
                    }
                    else if(compare == -1){
                        result += temp + ",";
                    }
                    //We ignore any instance where the versions are the same, as this means its a repeat
                }
            }
        }
        if(result.length() > 0){
            result = result.substring(0, result.length()-1);
        }
        return new String[] {currentLow, result};
    }

    /**
     * Compares the two versions. If version 1 is higher it will return true. 
     * Otherwise it will return false
     * @param version1
     * @param version2
     * @return
     */
    private int compareVersions(String version1, String version2){
        if(Double.parseDouble(version1) > Double.parseDouble(version2)){
            return 1;
        }
        else if(Double.parseDouble(version1) == Double.parseDouble(version2)){
            return 0;
        }

        return -1;
    }
}

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
    
}

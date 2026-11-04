/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.installation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.test.image.util.FileUtils;

/**
 * Pointer to a server profile.
 */
public class ServerProfile {
    public static final String CLASS_NAME = ServerProfile.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //

    public ServerProfile(String path, String name) throws Exception {
        this.path = path;
        this.name = name;

        List<String> useFeatures;
        Exception captured;
        try {
            useFeatures = listFeatures();
            captured = null;
        } catch ( Exception e ) {
            useFeatures = null;
            captured = e;
        }
        this.features = useFeatures;
        
        if ( captured != null ) {
            throw captured;
        }
    }


    //

    private final String path;
    private final String name;

    public String getName() {
        return name;
    }
    
    public String getPath() {
        return path;
    }

    //

    private List<String> features;
        
    public List<String> getFeatures() throws Exception {
        if ( features == null ) {
            throw new Exception("Prior failure to list features [ " + getPath() + " ]");        
        }
        return features;
    }

    protected List<String> listFeatures() throws IOException {
        String featuresPath = getPath() + "/features.xml";
        log("Listing features [ " + getName() + " ]: [ " + featuresPath + " ]");
        
        List<String> useFeatures = new ArrayList<String>();

        List<String> featureLines = FileUtils.load(featuresPath);
        for ( String featureLine : featureLines ) {
            featureLine = featureLine.trim();
            if ( !featureLine.startsWith("<feature>") ) {
                throw new IOException("Reading [ " + featuresPath + " ]: Feature line [ " + featureLine + " ] does not start with '<feature>'");
            } else if ( !featureLine.endsWith("</feature>") ) {
                throw new IOException("Reading [ " + featuresPath + " ]: Feature line [ " + featureLine + " ] does not end with '</feature>'");
            }
            String feature = featureLine.substring(
                    "<feature>".length(),
                    featureLine.length() - "</feature>".length() );
            feature = feature.trim();

            useFeatures.add(feature);
            log("  Feature [ " + feature + " ]");            
        }

        return useFeatures;
    }
}
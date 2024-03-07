/*******************************************************************************
 * Copyright (c) 2016, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.test.image.repo;

import java.io.File;

/**
 * Pointer to a maven feature repository.
 */
public class MavenRepository extends Repository {
    @SuppressWarnings("hiding")
    public static final String CLASS_NAME = MavenRepository.class.getSimpleName();

    public void log(String message) {
        log(CLASS_NAME, message);
    }

    //

    public MavenRepository(String imagePath, String path) throws Exception {
        super(imagePath, path);
    }

    //
    
    private String[] featureNames;
    
    public String[] getFeatureNames() {
        return featureNames;
    }

    //

    public static String[] listFeatures(String path) {
        return (new File(path)).list( MavenRepository::isFeature );
    }

    public static boolean isFeature(File parent, String name) {
        return ( name.startsWith("features-") && name.endsWith(".json") );
    }

    public static final String FEATURES_PATH = "com/ibm/websphere/appserver/features/features";
    
    public String getFeaturesPath() {
        return getPath() + "/" + FEATURES_PATH;
    }

    @Override
    public void setup() throws Exception {
        super.setup();

        String featuresPath = getFeaturesPath();
        log("Features [ " + featuresPath + " ]");

        String[] useFeatureNames = listFeatures(featuresPath);
        log("Features count [ " + featureNames.length + " ]");

        this.featureNames = useFeatureNames;
    }
}

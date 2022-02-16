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
package com.ibm.ws.image.test.topo;

import static com.ibm.ws.image.test.util.FileUtils.load;
import static com.ibm.ws.image.test.util.FileUtils.normalize;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Build control properties.
 */
@SuppressWarnings("null")
public class BuildProperties {
    public static final String CLASS_NAME = BuildProperties.class.getSimpleName();

    public static void log(String message) {
        System.out.println(message);
    }

    // e.g. 17.0.0.1 or 2017.0.0.1

    public static final String GA_VERSION_PROPERTY_NAME = "liberty.service.version"; // OK
    public static final String GA_VERSION = System.getProperty(GA_VERSION_PROPERTY_NAME);

    public static final String BETA_VERSION_PROPERTY_NAME = "liberty.beta.version"; // ?
    public static final String BETA_VERSION = System.getProperty(BETA_VERSION_PROPERTY_NAME);

    public static final String IFIX_VERSION_BASE_PROPERTY_NAME = "ifix.version.base"; // OK
    public static final String IFIX_VERSION_BASE = System.getProperty(IFIX_VERSION_BASE_PROPERTY_NAME);
    public static final boolean IFIX_BUILD = ( IFIX_VERSION_BASE != null );
    
    static {
        log("GA version   [ " + GA_VERSION_PROPERTY_NAME + " ]: [ " + GA_VERSION + " ]");
        log("Beta version [ " + BETA_VERSION_PROPERTY_NAME + " ]: [ " + BETA_VERSION + " ]");
        log("IFIX version [ " + IFIX_VERSION_BASE_PROPERTY_NAME + " ]: [ " + IFIX_VERSION_BASE + " ]");        
    }

    public static final String CREATE_IM_REPO_PROPERTY_NAME = "create.im.repo"; // WS-CD    
    public static final boolean CREATE_IM_REPO = Boolean.getBoolean(CREATE_IM_REPO_PROPERTY_NAME);

    public static final String BUILD_LICENSE_ZIP_PROPERTY_NAME = "build.license.zip"; // WS-CD 
    public static final boolean BUILD_LICENSE_ZIP = Boolean.getBoolean(BUILD_LICENSE_ZIP_PROPERTY_NAME);

    public static final String BUILD_ALWAYS_PROPERTY_NAME = "build.always";
    public static final boolean BUILD_ALWAYS = Boolean.getBoolean(BUILD_ALWAYS_PROPERTY_NAME); // Fold into all

    static {
        log("Create IM repository [ " + CREATE_IM_REPO_PROPERTY_NAME + " ]: [ " + CREATE_IM_REPO + " ]");
        log("Build license zip    [ " + BUILD_LICENSE_ZIP_PROPERTY_NAME + " ]: [ " + BUILD_LICENSE_ZIP + " ]");
        log("Build always         [ " + BUILD_ALWAYS_PROPERTY_NAME + " ]: [ " + BUILD_ALWAYS + " ]");
    }
    
    //
    
    public static final String IMAGES_PROPERTY_NAME = "image.output.upload.dir";
    public static final String IMAGES_DEFAULT = "../build.image/output/upload/externals/installables";

    public static final String IMAGES_PATH;
    public static final String[] IMAGE_PATHS;

    static {
        String useImagesPath = System.getProperty(IMAGES_PROPERTY_NAME, IMAGES_DEFAULT);
        File useImagesDir = new File(useImagesPath);
        useImagesPath = normalize( useImagesDir.getAbsolutePath() );

        if ( !useImagesDir.exists() ) {
            fail("Image directory [ " + useImagesPath + " ] does not exist");
        } else if ( !useImagesDir.isDirectory() ) {
            fail("Image directory [ " + useImagesPath + " ] is not a directory");
        } else {
            log("Image directory [ " + useImagesPath + " ]");
        }

        String[] useImagePaths = useImagesDir.list( BuildProperties::isImage );
        if ( useImagePaths == null ) {
            fail("Image directory [ " + useImagesPath + " ] could not be accessed");
        } else if ( useImagePaths.length == 0 ) {
            fail("Image directory [ " + useImagesPath + " ] is empty");
        }

        if ( File.separatorChar == '\\' ) {
            normalize(useImagePaths);
        }

        log("Images:");
        for ( String imagePath : useImagePaths ) {
            log("[ " + imagePath + " ]");
        }

        IMAGES_PATH = useImagesPath;
        IMAGE_PATHS = useImagePaths;
    }

    // There are too many of these to have an explicit list.
    public static boolean isImage(File parent, String name) {
        return ( name.endsWith(".jar") || name.endsWith(".zip") );
    }
    
    //
    
    public static final String GA_FEATURES_PATH = "../prereq.published/build/gaFeatures.txt";
    public static final String GA_FEATURES_PATH_ABS;
    public static final List<String> GA_FEATURE_NAMES;

    // From WS-CD-Open 22.002:
    //
    // betaFeatures-wasliberty.txt
    // betaFeatures.txt
    // gaFeatures-wasliberty.txt
    // gaFeatures.txt

    static {
        File gaFeaturesFile = new File(GA_FEATURES_PATH);
        String gaFeaturesAbsPath = gaFeaturesFile.getAbsolutePath();

        if ( !gaFeaturesFile.exists() ) {
            fail("GA features file [ " + gaFeaturesAbsPath + " ] does not exist");
        }

        List<String> gaFeatures;
        try {
            gaFeatures = load(gaFeaturesFile);
        } catch ( IOException e ) {
            fail("Failed to load GA features [ " + gaFeaturesAbsPath + " ]: " + e.getMessage());
            throw new RuntimeException(e);
        }

        GA_FEATURES_PATH_ABS = gaFeaturesAbsPath;
        GA_FEATURE_NAMES = gaFeatures;
    }    
}

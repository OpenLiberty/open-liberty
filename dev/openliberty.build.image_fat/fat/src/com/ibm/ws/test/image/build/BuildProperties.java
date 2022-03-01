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
package com.ibm.ws.test.image.build;

import static com.ibm.ws.test.image.util.FileUtils.match;
import static com.ibm.ws.test.image.util.FileUtils.normalize;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Build control properties.
 */
public class BuildProperties {
    public static final String CLASS_NAME = BuildProperties.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
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

// WS-CD
//    public static final String CREATE_IM_REPO_PROPERTY_NAME = "create.im.repo"; // WS-CD    
//    public static final boolean CREATE_IM_REPO = Boolean.getBoolean(CREATE_IM_REPO_PROPERTY_NAME);
//
//    public static final String BUILD_LICENSE_ZIP_PROPERTY_NAME = "build.license.zip"; // WS-CD 
//    public static final boolean BUILD_LICENSE_ZIP = Boolean.getBoolean(BUILD_LICENSE_ZIP_PROPERTY_NAME);
//
//    public static final String BUILD_ALWAYS_PROPERTY_NAME = "build.always";
//    public static final boolean BUILD_ALWAYS = Boolean.getBoolean(BUILD_ALWAYS_PROPERTY_NAME); // Fold into all
//
//    static {
//        log("Create IM repository [ " + CREATE_IM_REPO_PROPERTY_NAME + " ]: [ " + CREATE_IM_REPO + " ]");
//        log("Build license zip    [ " + BUILD_LICENSE_ZIP_PROPERTY_NAME + " ]: [ " + BUILD_LICENSE_ZIP + " ]");
//        log("Build always         [ " + BUILD_ALWAYS_PROPERTY_NAME + " ]: [ " + BUILD_ALWAYS + " ]");
//    }
    
    //
    
    //
    
    // basedir C:\dev\repos-pub\o-l\dev\openliberty.build.image_fat\build\libs\autoFVT
    // dir.build   C:\dev\repos-pub\o-l\dev\openliberty.build.image_fat\build\libs\autoFVT\build
    // liberty.location    ../../../../build.image/wlp

    public static final String BASE_PROPERTY_NAME = "basedir";
    public static final String BASE_DEFAULT = ".";
    public static final String BASE_PATH;
    public static final File BASE_DIR;

    public static final String BUILD_PROPERTY_NAME = "dir.build";
    public static final String BUILD_DEFAULT = "./build";
    public static final String BUILD_PATH;
    public static final File BUILD_DIR;

    public static final String LIBERTY_PROPERTY_NAME = "liberty.location";
    public static final String LIBERTY_DEFAULT = "../../../../build.image/wlp";
    public static final String LIBERTY_PATH;
    public static final File LIBERTY_DIR;

    // Relative to 'LIBERTY_PATH'    
    public static final String IMAGES_RELATIVE_PATH = "../build/libs/distributions/";

    public static final String IMAGES_PATH;
    public static final File IMAGES_DIR;

    public static final String[] IMAGE_NAMES;

    protected static String getNormalPath(String tag, String path) {
        File file = new File(path);
        try {
            path = file.getCanonicalPath();
        } catch ( IOException e ) {
            log("Failed to obtain [ " + tag + " ] canonical path [ " + path + " ]");
        }
        path = normalize(path);
        log("[ " + tag + " ] directory [ " + path + " ]");
        return path;
    }
    
    static {
        String basePath = System.getProperty(BASE_PROPERTY_NAME, BASE_DEFAULT);
        BASE_PATH = getNormalPath("Base", basePath);
        BASE_DIR = new File(BASE_PATH);

        String buildPath = System.getProperty(BUILD_PROPERTY_NAME, BUILD_DEFAULT);
        BUILD_PATH = getNormalPath("Build", buildPath);
        BUILD_DIR = new File(BUILD_PATH);

        String libertyPath = System.getProperty(LIBERTY_PROPERTY_NAME, LIBERTY_DEFAULT);
        LIBERTY_PATH = getNormalPath("Liberty", libertyPath);
        LIBERTY_DIR = new File(LIBERTY_PATH);

        String imagesPath = LIBERTY_PATH + '/' + IMAGES_RELATIVE_PATH;
        IMAGES_PATH = getNormalPath("Images", imagesPath);
        IMAGES_DIR = new File(IMAGES_PATH);

        IMAGE_NAMES = validateListing("Images", IMAGES_DIR, BuildProperties::isImage);
    }

    public static boolean isImage(File parent, String name) {
        return ( name.endsWith(".jar") || name.endsWith(".zip") );
    }

    public static List<String> getImageNames(String[] parts) {
        List<String> images = null;
        for ( String imagePath : IMAGE_NAMES ) {
            int slashLoc = imagePath.lastIndexOf(File.separatorChar);
            String imageName = ( (slashLoc == -1) ? imagePath : imagePath.substring(slashLoc + 1) );
            if ( !match(parts, imageName) ) {
                continue;
            }

            if ( images == null ) {
                images = new ArrayList<>(1);
            }
            images.add(imagePath);
        }
        return images;
    }

    // WS-CD-Open
    
    //

//    public static final String GA_FEATURES_PATH = "../prereq.published/build/gaFeatures.txt";
//    public static final String GA_FEATURES_PATH_ABS;
//    public static final List<String> GA_FEATURE_NAMES;
//
//    // From WS-CD-Open 22.002:
//    //
//    // betaFeatures-wasliberty.txt
//    // betaFeatures.txt
//    // gaFeatures-wasliberty.txt
//    // gaFeatures.txt
//
//    static {
//        File gaFeaturesFile = new File(GA_FEATURES_PATH);
//        String gaFeaturesAbsPath = gaFeaturesFile.getAbsolutePath();
//
//        if ( !gaFeaturesFile.exists() ) {
//            throw new Exception("GA features file [ " + gaFeaturesAbsPath + " ] does not exist");
//        }
//
//        List<String> gaFeatures;
//        try {
//            gaFeatures = load(gaFeaturesFile);
//        } catch ( IOException e ) {
//            throw new Exception("Failed to load GA features [ " + gaFeaturesAbsPath + " ]: " + e.getMessage());
//            throw new RuntimeException(e);
//        }
//
//        GA_FEATURES_PATH_ABS = gaFeaturesAbsPath;
//        GA_FEATURE_NAMES = gaFeatures;
//    }
    
    // C:/dev/repos-pub/o-l/dev/openliberty.build.image_fat/build/libs/autoFVT/
    // ../build.image/build/libs/distributions

    // Relative to 'LIBERTY_PATH'    
    public static final String PROFILES_RELATIVE_PATH = "../build/libs/distributions/";

    public static final String PROFILES_PATH;
    public static final File PROFILES_DIR;

    public static final String[] PROFILE_NAMES;

    public static String getProfilesPath() {
        return PROFILES_PATH;
    }

    public static String[] getProfileNames() {
        return PROFILE_NAMES;
    }
    
    public static boolean isProfile(File parent, String name) {
        return true;
    }
    
    static {
        String profilesPath = LIBERTY_PATH + '/' + PROFILES_RELATIVE_PATH;
        PROFILES_PATH = getNormalPath("Profiles", profilesPath);
        PROFILES_DIR = new File(PROFILES_PATH);
        
        PROFILE_NAMES = validateListing("Profiles", PROFILES_DIR, BuildProperties::isProfile);
    }

    //

    protected static File validateDirectory(String tag, String path, String altPath) {
        File dir = new File(path);
        path = normalize( dir.getAbsolutePath() );

        if ( !dir.exists() ) {
            log(tag + " directory [ " + path + " ] does not exist; trying alternate");
            path = altPath;
            dir = new File(path);
            path = normalize( dir.getAbsolutePath() );
            
            if ( !dir.exists() ) {
                dir = null;
                log(tag + " directory [ " + path + " ] does not exist");
            }            
        }

        if ( dir != null ) {
            if ( !dir.isDirectory() ) {
                dir = null;
                log(tag + " directory [ " + path + " ] is not a directory");
            } else {
                log(tag + " directory [ " + path + " ]");
            }
        }
        
        return dir;
    }

    protected static String[] validateListing(String tag, File dir, FilenameFilter filter) {
        String path = dir.getPath();

        String[] names;
        String message;

        if ( !dir.exists() ) {
            names = null;
            message = "does not exist";
        } else if ( !dir.isDirectory() ) {
            names = null;
            message = "is not a directory";

        } else {
            names = dir.list(filter);

            if ( names == null ) {
                message = "could not be accessed";
            } else if ( names.length == 0 ) {
                message = "is empty";

            } else {
                message = null;

                if ( File.separatorChar == '\\' ) {
                    normalize(names);
                }

                log("[ " + tag + " ]:");
                for ( String name : names ) {
                    log("  [ " + name + " ]");
                }
            }
        }

        log("[ " + tag + " ] directory [ " + path + " ] " + message);

        if ( names == null ) {
            names = new String[] {};
        }
        return names;
    }
}

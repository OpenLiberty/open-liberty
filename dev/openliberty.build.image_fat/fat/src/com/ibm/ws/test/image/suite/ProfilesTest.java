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
package com.ibm.ws.test.image.suite;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.test.image.build.BuildProfiles;
import com.ibm.ws.test.image.build.BuildProperties;
import com.ibm.ws.test.image.installation.ServerProfile;

import componenttest.custom.junit.runner.FATRunner;

@RunWith(FATRunner.class)
public class ProfilesTest {
    public static final String CLASS_NAME = InstallServerTest.class.getSimpleName();
    
    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    public static final String BANNER =
            "============================================================";
    
    public static void logBeginning(String message) {
        log(message);
        log(BANNER);
    }
    
    public static void logEnding(String message) {
        log(BANNER);
        log(message);
    }
    
    //

    public static final String[] EXPECTED_PROFILE_NAMES = {
            "javaee8",
            "microProfile4",
            "webProfile8",

            "jakartaee9",
            "microProfile5",
            "webProfile9"
    };
    
    public static Set<String> toSet(String[] valuesArray) {
        Set<String> valuesSet = new HashSet<>(valuesArray.length);
        for ( String value : valuesArray ) {
            valuesSet.add(value);
        }
        return valuesSet;
    }
    
    public static Set<String> without(Set<String> set1, Set<String> set2) {
        Set<String> without = null;
        
        for ( String value : set1 ) {
            if ( !set2.contains(value) ) {
                if ( without == null ) {
                    without = new HashSet<>();
                }
                without.add(value);
            }
        }
        
        return without;
    }

    @Test
    public void testVerifyProfiles() throws Exception {
        logBeginning("Verifying profiles");
        try {
            verifyProfiles();
        } finally {
            logEnding("Verified profiles");
        }
    }
    
    protected void verifyProfiles() throws Exception {
        log("Profiles path     [ " + BuildProperties.PROFILES_PATH + " ]");
        log("Expected profiles [ " + Arrays.toString(EXPECTED_PROFILE_NAMES) + " ]");
        log("Actual profiles   [ " + Arrays.toString(BuildProperties.PROFILE_NAMES) + " ]");        

        Set<String> expectedProfiles = toSet(EXPECTED_PROFILE_NAMES);
        Set<String> actualProfiles = toSet(BuildProperties.PROFILE_NAMES);
    
        Set<String> missingProfiles = without(expectedProfiles, actualProfiles);
        Set<String> extraProfiles = without(actualProfiles, expectedProfiles);
    
        String missingMessage;
        if ( missingProfiles != null ) {
            log(missingMessage = "Missing profiles [ " + missingProfiles + " ].");
        } else {
            missingMessage = null;
        }

        String extraMessage;
        if ( extraProfiles != null ) {
            log(extraMessage = "Extra profiles [ " + extraProfiles + " ].");            
        } else {
            extraMessage = null;
        }
        
        if ( (missingMessage != null) || (extraMessage != null) ) {
            String message;
            if ( missingMessage == null ) {
                message = extraMessage;
            } else if ( extraMessage == null ) {
                message = missingMessage;
            } else {
                message = missingMessage + "  " + extraMessage;
            }
            Assert.fail(message);
        }
    }
    
    @Test
    public void testVerifyProfileImages() throws Exception {
        logBeginning("Verifying profile images");
        try {
            verifyProfileImages();
        } finally {
            logEnding("Verified profile images");
        }
    }
    
    protected void verifyProfileImages() throws Exception {
        log("Actual profiles [ " + Arrays.toString(BuildProperties.PROFILE_NAMES) + " ]");
        log("Images path [ " + BuildProperties.IMAGES_PATH + " ]");

        boolean failed = false;

        // <prefix>-<profileName>-<iteration>-<timeStamp><suffix>
        //
        // openliberty-jakartaee9-22.0.0.1-202202171441.zip

        String[] imageParts = {
                "openliberty", // Required prefix
                null,          // Replaced with the profile name
                null,          // Match anything
                null,          // Match anything
                ".zip"         // Required suffix
        };
        
        for ( String profileName : BuildProperties.PROFILE_NAMES ) {
            imageParts[1] = profileName;
            List<String> imageNames = BuildProperties.getImageNames(imageParts);
            
            if ( imageNames == null ) {
                failed = true;
                log("Profile [ " + profileName + " ] failed to locate image.");
            } else if ( imageNames.size() > 1 ) {
                failed = true;
                log("Profile [ " + profileName + " ] located too many images [ " + imageNames + " ]");
            } else {
                log("Profile [ " + profileName + " ] located image [ " + imageNames.get(0) + " ]");
            }
        }
        
        if ( failed ) {
            Assert.fail("Failed to locate a singleton image for each profile.");
        }
    }
    
    @Test
    public void testProfileFeatures() throws Exception {
        logBeginning("Verifying profile features");
        try {
            verifyProfileFeatures();
        } finally {
            logEnding("Verified profile features");
        }
    }
    
    protected void verifyProfileFeatures() throws Exception {
        log("Profiles path [ " + BuildProperties.PROFILES_PATH + " ]");
        log("Actual profiles [ " + Arrays.toString(BuildProperties.PROFILE_NAMES) + " ]");

        String firstFailure = null;

        for ( String profileName : BuildProperties.PROFILE_NAMES ) {
            ServerProfile serverProfile = BuildProfiles.getProfile(profileName);
            if ( serverProfile == null ) {
                String message = "Failed to load profile [ " + profileName + " ]";
                if ( firstFailure == null ) {
                    firstFailure = message;
                }
                log(message);
            } else {
                log(serverProfile);
            }
        }

        if ( firstFailure != null ) {
            throw new Exception(firstFailure);
        }
    }

    protected void log(ServerProfile profile) throws Exception {
        log("Profile [ " + profile.getName() + " ] at [ " + profile.getPath() + " ]");
        log("Features:");
        for ( String featureName : profile.getFeatures() ) {
            log("  [ " + featureName + " ]");
        }
    }
}

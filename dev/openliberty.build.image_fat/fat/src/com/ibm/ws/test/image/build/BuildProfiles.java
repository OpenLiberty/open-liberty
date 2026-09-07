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
package com.ibm.ws.test.image.build;

import java.util.HashMap;
import java.util.Map;

import com.ibm.ws.test.image.installation.ServerProfile;

public class BuildProfiles {
    public static final String CLASS_NAME = BuildProfiles.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    public static final Map<String, ServerProfile> PROFILES;
    static {
        Map<String, ServerProfile> profiles = new HashMap<>(BuildProperties.PROFILE_NAMES.length);
        for ( String profileName : BuildProperties.PROFILE_NAMES ) {
            String profilePath = BuildProperties.PROFILES_PATH + '/' + profileName;
            ServerProfile profile;
            try {
                profile = new ServerProfile(profilePath, profileName);
            } catch ( Exception e ) {
                profile = null;
                log("Failed to load server profile [ " + profilePath+ " ]: " + e.getMessage());
            }
            if ( profile != null ) {
                profiles.put(profileName, profile);
            }
        }
        
        PROFILES = profiles;
    }
    
    public static ServerProfile getProfile(String profileName) {
        return PROFILES.get(profileName);
    }
}

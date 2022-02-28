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

import static com.ibm.ws.test.image.util.FileUtils.IS_WINDOWS;
import static com.ibm.ws.test.image.util.FileUtils.load;
import static com.ibm.ws.test.image.util.FileUtils.selectMissing;
import static com.ibm.ws.test.image.util.ProcessRunner.StreamCopier;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import org.junit.Assert;

import com.ibm.ws.test.image.Timeouts;
import com.ibm.ws.test.image.util.FileUtils;
import com.ibm.ws.test.image.util.ProcessRunner;
import com.ibm.ws.test.image.util.ScriptFilter;
import com.ibm.ws.test.image.util.XMLUtils;

/**
 * Pointer to a server profile.
 */
public class ServerProfile {
    public static final String CLASS_NAME = ServerProfile.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //

    public ServerProfile(String path) {
        this.path = path;
        
        int lastSlash = path.lastIndexOf( File.separatorChar );
        this.name = ( (lastSlash == -1) ? path : path.substring(lastSlash + 1) ); 

        this.features = null;
        this.isSetFeatures = false;
    }

    private final String path;

    public String getPath() {
        return path;
    }

    private final String name;
    
    public String getName() {
        return name;
    }

    private List<String> features;
    private boolean isSetFeatures;
    
    public List<String> getFeatures() throws Exception {
        if ( !isSetFeatures ) {
            Exception captured;
            try {
                features = listFeatures();
                captured = null;
            } catch ( Exception e ) {
                features = null;
                captured = e;
            }
            
            isSetFeatures = true;
            if ( captured != null ) {
                throw captured;
            }
        }
        
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
            if ( !featureLine.startsWith("<feature>") ) {
                throw new IOException("Reading [ " + featuresPath + " ]: Feature line [ " + featureLine + " ] does not start with '<feature>'");
            } else if ( !featureLine.endsWith("</feature>") ) {
                throw new IOException("Reading [ " + featuresPath + " ]: Feature line [ " + featureLine + " ] does not end with '</feature>'");
            }
            String feature = featureLine.substring(
                    "<feature>".length(),
                    featureLine.length() - "</feature>".length() );

            useFeatures.add(feature);
            log("  Feature [ " + feature + " ]");            
        }

        return useFeatures;
    }
}
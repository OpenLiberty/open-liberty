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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Manifest;

/**
 * An installed server feature
 */
public class ServerFeature {
    public static final String CLASS_NAME = ServerFeature.class.getSimpleName();

    public static void log(String message) {
        System.out.println(CLASS_NAME + ": " + message);
    }

    //

    public ServerFeature(String path, String mfName) throws Exception {
        this.path = path;

        String useFullName = mfName.substring(0, mfName.length() - ".mf".length());

        int dashOffset = useFullName.indexOf('-');
        
        String useName;
        String useVersion;
        if ( dashOffset == -1 ) {
            useName = useFullName;
            useVersion = null;
        } else {
            useName = useFullName.substring(0, dashOffset);
            useVersion = useFullName.substring(dashOffset + 1);
        }

        // Almost all of the features have a "-", except for one:
        //     "io.openliberty.adminCenter1.0.javaee.mf"
        //
        // if ( dashOffset == -1 ) {
        //     throw new Exception("Server feature is not valid [ " + path + " ] [ " + mfName + " ]");
        // }
        
        this.fullName = useFullName;
        this.name = useName;
        this.version = useVersion;

        this.manifest = null;

        this.rawSymbolicName = null;
        this.symbolicName = null;

        this.rawSubsystemName = null;
        this.subsystemName = null;
        
        this.rawSubsystemVersion = null;
        this.subsystemVersion = null;
    }

    private final String path;

    public String getPath() {
        return path;
    }

    public String getFullPath() {
        return getPath() + '/' + getFullName() + ".mf";
    }
    
    private final String fullName;
    
    public String getFullName() {
        return fullName;
    }
    
    private final String name;
    
    public String getName() {
        return name;
    }

    private final String version;
    
    public String getVersion() {
        return version;
    }

    //

    private Manifest manifest;

    public Manifest getManifest() {
        if ( manifest == null ) {
            throw new RuntimeException("Feature manifest has not been loaded [ " + getFullPath() + " ]");
        }
        return manifest;
    }

    public String getMainAttribute(String attributeName) {
        return getManifest().getMainAttributes().getValue(attributeName);
    }

    private String rawSymbolicName;
    private String symbolicName;

    private String rawSubsystemName;
    private String subsystemName;

    private String rawSubsystemVersion;
    private String subsystemVersion;

    public String getRawSymbolicName() {
        return rawSymbolicName;
    }

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getRawSubsystemName() {
        return rawSubsystemName;
    }
    
    public String getSubsystemName() {
        return subsystemName;
    }
    
    public String getRawSubsystemVersion() {
        return rawSubsystemVersion;
    }
    
    public String getSubsystemVersion() {
        return subsystemVersion;
    }
    
    public void loadManifest() throws Exception {
        Manifest useManifest;
        Exception captured;
        try {
            useManifest = readManifest();
            captured = null;
        } catch ( Exception e ) {
            useManifest = new Manifest();
            captured = e;
        }
        manifest = useManifest;

        if ( captured != null ) {
            throw captured;
        }

        setSymbolicName();
        setSubsystemName();
        setSubsystemVersion();
    }

    protected void setSymbolicName() {
        String useRaw = getMainAttribute("Subsystem-SymbolicName");
        if ( useRaw == null ) {
            rawSymbolicName = null;
            symbolicName = null;
        } else {
            String useCore = getCore(useRaw);
            rawSymbolicName = useRaw;
            symbolicName = useCore;
        }
    }

    protected void setSubsystemName() {
        String useRaw = getMainAttribute("Subsystem-Name");
        if ( useRaw == null ) {
            rawSubsystemName = null;
            subsystemName = null;
        } else {
            String useCore = getCore(useRaw);
            rawSubsystemName = useRaw;
            subsystemName = useCore;       
        }
    }
    
    protected void setSubsystemVersion() throws Exception {
        String useRaw = getMainAttribute("Subsystem-Version");
        if ( useRaw == null ) {
            rawSubsystemVersion = null;
            subsystemVersion = null;
        } else {
            String useCore = getCore(useRaw);
            rawSubsystemVersion = useRaw;
            subsystemVersion = useCore;       
        }
    }
    
    protected static String getCore(String attributeValue) {
        String core;
        int offset = attributeValue.indexOf(';');
        if ( offset == -1 ) {
            core = attributeValue;
        } else {
            core = attributeValue.substring(0, offset);
        }
        return core.trim();
    }

    protected static String getTail(String attributeValue) {
        String tail;
        int offset = attributeValue.indexOf(';');
        if ( offset == -1 ) {
            tail = "";
        } else {
            tail = attributeValue.substring(offset + 1);
        }
        return tail.trim();
    }
    
    protected Manifest readManifest() throws IOException {
        String manifestPath = getFullPath();
        log("Reading manifest [ " + getName() + " ]: [ " + manifestPath + " ]");

        Manifest useManifest;
        try ( InputStream manifestStream = new FileInputStream(manifestPath) ) {
            useManifest = new Manifest(manifestStream);
        }
        
        return useManifest;
    }
}
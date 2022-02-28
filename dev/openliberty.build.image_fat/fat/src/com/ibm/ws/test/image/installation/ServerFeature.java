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

import java.io.File;
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

    public ServerFeature(String path) {
        this.path = path;
      
        int lastSlash = path.lastIndexOf( File.separatorChar );
        String mfName = ( (lastSlash == -1) ? path : path.substring(lastSlash + 1) ); 

        String useName;
        if ( !mfName.endsWith(".mf") ) {
            useName = mfName;
        } else {
            useName = mfName.substring(0, mfName.length() - ".mf".length());
        }
        this.name = useName;

        this.manifest = null;
        this.isSetManifest = false;
        
        this.rawSymbolicName = null;
        this.symbolicName = null;
        this.isSetSymbolicName = false;
        
        this.rawVersion = null;
        this.version = null;
        this.isSetVersion = false;
    }

    private final String path;

    public String getPath() {
        return path;
    }

    private final String name;
    
    public String getName() {
        return name;
    }

    private Manifest manifest;
    private boolean isSetManifest;
    
    public Manifest getManifest() throws Exception {
        if ( !isSetManifest) {
            Exception captured;
            try {
                manifest = readManifest();
                captured = null;
            } catch ( Exception e ) {
                manifest = null;
                captured = e;
            }
            
            isSetManifest = true;
            if ( captured != null ) {
                throw captured;
            }
        }
        
        if ( manifest == null ) {
            throw new Exception("Prior failure to read manifest [ " + getPath() + " ]");
        }
        return manifest;
    }

    public String getMainAttribute(String attributeName) throws Exception {
        return getManifest().getMainAttributes().getValue(attributeName);
    }

    private String rawSymbolicName;
    private String symbolicName;
    private boolean isSetSymbolicName;

    public String getRawSymbolicName() throws Exception {
        setSymbolicName();
        return rawSymbolicName;
    }
    
    public String getSymbolicName() throws Exception {
        setSymbolicName();
        return symbolicName;
    }
    
    protected void setSymbolicName() throws Exception {
        if ( isSetSymbolicName ) {
            return;
        }
        
        String useRaw = getMainAttribute("Subsystem-SymbolicName");
        String useCore = getCore(useRaw);
        
        rawSymbolicName = useRaw;
        symbolicName = useCore;
        isSetSymbolicName = true;
    }

    private String rawVersion;
    private String version;
    private boolean isSetVersion;

    public String getRawVersion() throws Exception {
        setVersion();
        return rawVersion;
    }
    
    public String getVersion() throws Exception {
        setVersion();
        return version;
    }
    
    protected void setVersion() throws Exception {
        if ( isSetVersion ) {
            return;
        }
        
        String useRaw = getMainAttribute("IBM-Feature-Version");
        String useCore = getCore(useRaw);

        rawVersion = useRaw;
        version = useCore;
        isSetVersion = true;
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
        String manifestPath = getPath();
        log("Reading manifest [ " + getName() + " ]: [ " + manifestPath + " ]");

        Manifest useManifest;
        try ( InputStream manifestStream = new FileInputStream( getPath() ) ) {
            useManifest = new Manifest(manifestStream);
        }
        
        return useManifest;
    }
}
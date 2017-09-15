/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.jsp.taglib.config;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to handle the enabling of a tag library that is available for all applications on a server.
 */
public class GlobalTagLibConfig {
    private String jarName = null;
    private URL jarURL = null;
    private ClassLoader classloader = null;
    private List tldPathList = null;
    
    public GlobalTagLibConfig() {
        tldPathList = new ArrayList();
    }
    
    /**
     * Returns a String containing the name of the jar
     * 
     * @return String - the name of the jar 
     */
    public String getJarName() {
        return jarName;
    }

    /**
     * Returns a List of all the tlds to be parsed within this jar
     * 
     * @return List - the tld files within the jar 
     */
    public List getTldPathList() {
        return tldPathList;
    }
    
    /**
     * Sets the jar name for this global tag library
     * 
     * @param string String - the name of the jar for this global tag library
     */
    public void setJarName(String string) {
        jarName = string;
    }

    /**
     * Sets the jar url for this global tag library
     * 
     * @param jarURL String - the url of the jar for this global tag library
     */
    public void setJarURL(URL jarURL) {
        this.jarURL = jarURL;
    }
    
    /**
     * Gets the jar url for this global tag library
     * 
     * @return URL - the url for this global tag library 
     */
    public URL getJarURL() {
        return jarURL;
    }
    
    /**
     * Gets the classloader for this global tag library
     * 
     * @return ClassLoader - the classloader for this global tag library 
     */
    public ClassLoader getClassloader() {
        return classloader;
    }

    /**
     * Sets the classloader for this global tag library
     * 
     * @param classloader ClassLoader - the classloader to be used for this global tag library 
     */
    public void setClassloader(ClassLoader classloader) {
        this.classloader = classloader;
    }
}

/*******************************************************************************
 * Copyright (c) 1997, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.wsspi.jsp.taglib.config;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used in conjunction with a GlobalTagLibConfig and is used to provide specific path 
 * information for a tld file.
 */
public class TldPathConfig {
    private String tldPath = null;
    private String uri = null;
    private boolean containsListenerDefs = false;
    private List availabilityConditionList = null;    
    private boolean overrideTLDURI = false;
    

    /**
     * Use {@link #TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs)  TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs)} instead
     * 
     * @param tldPath - location of the TLD file.
     * @param uri -This value is ignored in favor of the uri attribute within the TLD file. Use the other constructor if a custom URI is needed.
     * @param strContainsListenerDefs - use "true" if any listeners are contained while any other value is considered false.
     */
    @Deprecated 
    public TldPathConfig(String tldPath, String uri, String strContainsListenerDefs) {
        this.tldPath = tldPath;
        this.uri = uri;
        if (strContainsListenerDefs != null && strContainsListenerDefs.equalsIgnoreCase("true")) {
            containsListenerDefs = true;    
        }
        availabilityConditionList = new ArrayList();
        overrideTLDURI = false;
    }
    /**
     * Note that strContainsListenerDefs is a boolean
     * 
     * @param tldPath - location of the TLD file.
     * @param uri - overrides the uri attribute within the TLD file. (If overriding is not needed, set uri argument to match the uri-attribute in the TLD)
     * @param strContainsListenerDefs - boolean value  if any listeners are contained within the TLD.
     */
    public TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs) {
        this.tldPath = tldPath;
        this.uri = uri;
        this.containsListenerDefs = strContainsListenerDefs;  
        availabilityConditionList = new ArrayList();
        this.overrideTLDURI = true;
    }

    /**
     * Gets the conditions as to when this tld is made available.
     * The condition can be the existence of a file within the web-inf directory or the existence of a servlet class.
     * @return List - a list of availability conditions
     */
    public List getAvailabilityConditionList() {
        return availabilityConditionList;
    }

    /**
     * Gets the relative path within the jar to the tld file
     * @return String - the relative path within the jar
     */
    public String getTldPath() {
        return tldPath;
    }
    
    /**
     * Gets the uri of the tld
     * @return String - the uri of the tld
     */
    public String getUri() {
        return uri;
    }

    /**
     *  The uri will only be picked up by the JSP Engine when {@link #TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs)} is used.
     * 
     * Sets the uri for the tld
     * <p> The uri will only be picked up by the JSP Engine when {@link #TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs)} is used. </p>
     * @param string String - the uri for the tld
     */
    public void setUri(String string) {
        uri = string;
    }
    
    /**
     * Gets whether the tld contains any listener elements
     * return boolean - if the tld file contains any listener elements
     */
    public boolean containsListenerDefs() {
        return containsListenerDefs;
    }

    /**
     * Specifies if the uri argument should override the uri attribute in the TLD. 
     * <p> Determined by which construtor is used. </p>
     * @return boolean - true only if {@link #TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs) TldPathConfig(String tldPath, String uri, boolean strContainsListenerDefs)}  is used
     */
    public boolean isTLDURIOverridden() {
        return this.overrideTLDURI;
    }

}

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
package com.ibm.ws.jsp.taglib.config;

import java.util.ArrayList;
import java.util.List;

public class TldPathConfig {
    private String tldPath = null;
    private String uri = null;
    private boolean containsListenerDefs = false;
    private List availabilityConditionList = null;    
    
    public TldPathConfig(String tldPath, String uri, String strContainsListenerDefs) {
        this.tldPath = tldPath;
        this.uri = uri;
        if (strContainsListenerDefs != null && strContainsListenerDefs.equalsIgnoreCase("true")) {
            containsListenerDefs = true;    
        }
        availabilityConditionList = new ArrayList();
    }
    
    public List getAvailabilityConditionList() {
        return availabilityConditionList;
    }

    public String getTldPath() {
        return tldPath;
    }
    
    public String getUri() {
        return uri;
    }

    public void setUri(String string) {
        uri = string;
    }
    
    public boolean containsListenerDefs() {
        return containsListenerDefs;
    }
}

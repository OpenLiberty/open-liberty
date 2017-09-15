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
package com.ibm.ws.jsp.translator.utils;

public class TagFileId {
    private String prefix = null;
    private String uri = null;
    private String tagName = null;
    
    public TagFileId(String prefix, String uri, String tagName) {
        this.prefix = prefix;
        this.uri = uri;
        this.tagName = tagName;
    }
    
    /**
     * Returns the prefix.
     * @return String
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Returns the tagName.
     * @return String
     */
    public String getTagName() {
        return tagName;
    }

    /**
     * Returns the uri.
     * @return String
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the prefix.
     * @param prefix The prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the tagName.
     * @param tagName The tagName to set
     */
    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Sets the uri.
     * @param uri The uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public boolean equals(Object o) {
        boolean isEqual = false;
        
        if (o instanceof TagFileId) {
            TagFileId id = (TagFileId)o;
            if (id.getUri().equals(uri) && id.getTagName().equals(tagName)) 
                isEqual = true;
        }
            
        return (isEqual);
    }
    
    public String toString() {
        return "[" + getTagName() + " " + getUri() + " " + getPrefix()+"]";
    }
}

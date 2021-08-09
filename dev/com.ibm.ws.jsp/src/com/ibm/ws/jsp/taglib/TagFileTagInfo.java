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
package com.ibm.ws.jsp.taglib;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagLibraryInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;

public class TagFileTagInfo extends TagInfo {
    private String dynamicAttrsMapName = null;
    private String tagName = null;
    private String tagClassName = null;
    private String bodyContent = null;
    private String infoString = null;
    private TagLibraryInfo taglib = null;
    private TagExtraInfo tagExtraInfo = null;
    private TagAttributeInfo[] attributeInfo = null;
    private String displayName = null;
    private String smallIcon = null;
    private String largeIcon = null;
    private TagVariableInfo[] tvi = null;
    private String mapName = null;
    
    public TagFileTagInfo(String tagName,
                          String tagClassName,
                          String bodyContent,
                          String infoString,
                          TagLibraryInfo taglib,
                          TagExtraInfo tagExtraInfo,
                          TagAttributeInfo[] attributeInfo,
                          String displayName,
                          String smallIcon,
                          String largeIcon,
                          TagVariableInfo[] tvi,
                          String mapName) {
        super(tagName, 
              tagClassName, 
              bodyContent, 
              infoString, 
              taglib,
              tagExtraInfo, 
              attributeInfo, 
              displayName, 
              smallIcon, 
              largeIcon,
              tvi);                              
        this.dynamicAttrsMapName = mapName;
        this.tagName=tagName;
        this.tagClassName=tagClassName;
        this.bodyContent=bodyContent;
        this.infoString=infoString;
        this.taglib=taglib;
        this.tagExtraInfo=tagExtraInfo;
        this.attributeInfo=attributeInfo;
        this.displayName=displayName;
        this.smallIcon=smallIcon;
        this.largeIcon=largeIcon;
        this.tvi=tvi;
        this.mapName=mapName;
    }
    
    public String getDynamicAttributesMapName() {
        return dynamicAttrsMapName;
    }

    public boolean hasDynamicAttributes() {
        return dynamicAttrsMapName != null;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("TagLibraryInfo name [");
        if (taglib.getShortName() != null)
            sb.append(taglib.getShortName());
        sb.append("] info [");
        if (taglib.getInfoString() != null)
            sb.append(taglib.getInfoString());
        sb.append("] urn [");
        if (taglib.getReliableURN() != null)
            sb.append(taglib.getReliableURN());
        sb.append("] jspversion [");
        if (taglib.getRequiredVersion() != null)
            sb.append(taglib.getRequiredVersion());
        sb.append("]\n");
        
        sb.append("TagFileInfo tagName [");
        if (tagName != null)
            sb.append(tagName);
        sb.append("] tagClassName [");
        if (tagClassName != null)
            sb.append(tagClassName);
        sb.append("] bodyContent [");
        if (bodyContent != null)
            sb.append(bodyContent);
        sb.append("] infoString [");
        if (infoString != null)
            sb.append(infoString);
        sb.append("] displayName [");
        if (displayName != null)
            sb.append(displayName);
        sb.append("] smallIcon [");
        if (smallIcon != null)
            sb.append(smallIcon);
        sb.append("] largeIcon [");
        if (largeIcon != null)
            sb.append(largeIcon);
        sb.append("] mapName [");
        if (mapName != null)
            sb.append(mapName);
        sb.append("] hasDynamicAttributes [");
        sb.append(hasDynamicAttributes());
        sb.append("]\n");
        if (this.attributeInfo != null) {
            TagLibraryInfoImpl.printTagAttributeInfo(sb, attributeInfo);
        }
        if (this.tvi != null) {
            TagLibraryInfoImpl.printTagVariableInfo(sb, tvi);
        }        
        
        return (sb.toString());
    }
}

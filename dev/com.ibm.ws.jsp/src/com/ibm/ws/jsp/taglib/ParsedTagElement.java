/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp.taglib;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.jsp.tagext.TagAttributeInfo;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.TagInfo;
import javax.servlet.jsp.tagext.TagVariableInfo;

import com.ibm.ws.jsp.JspCoreException;

/**
 *
 */
public class ParsedTagElement {
    String tldLocation = null;
    String tagName;
    String tagClassName;
    String bodyContent;
    String tagDescription;
    TagLibraryInfoImpl tli;
    String teiClassName;
    TagAttributeInfo[] tagAttributes;
    String displayName;
    String smallIcon;
    String largeIcon;
    TagVariableInfo[] tagVariables;
    boolean dynamicAttributes;
    
    
    static final protected Logger logger = Logger.getLogger("com.ibm.ws.jsp");;
    static final protected Level logLevel = Level.FINEST;
    private static final String CLASS_NAME="com.ibm.ws.jsp.taglib.ParsedTagElement";

    public ParsedTagElement(String tldLocation, String tagName, String tagClassName, String bodyContent, String tagDescription, TagLibraryInfoImpl tli, 
                            String teiClassName, TagAttributeInfo[] tagAttributes, String displayName, String smallIcon, 
                            String largeIcon, TagVariableInfo[] tagVariables, boolean dynamicAttributes) {
        this.tldLocation = tldLocation;
        this.tagName = tagName;
        this.tagClassName = tagClassName;
        this.bodyContent = bodyContent;
        this.tagDescription = tagDescription;
        this.tli = tli;
        this.teiClassName = teiClassName;
        this.tagAttributes = tagAttributes;
        this.displayName = displayName;
        this.smallIcon = smallIcon;
        this.largeIcon = largeIcon;
        this.tagVariables = tagVariables;
        this.dynamicAttributes = dynamicAttributes;
    }
    
    public String getTagClassName() {
        return tagClassName;
    }
    
    public TagInfo createTagObject(ClassLoader classloader) {
        TagExtraInfo tei = null;
        if (teiClassName != null) {
            // begin  221334: check if user specified empty tag for tei-class and log warning.
            if(teiClassName.trim().equals("")){
                logger.logp(Level.WARNING, CLASS_NAME, "endElement", "TagExtraInfo specified in tld without a value.  tld=[" + tldLocation +"]");
            }
            else{
                // end  221334: check if user specified empty tag for tei-class and log warning.        
                try {
                    tei = (TagExtraInfo)classloader.loadClass(teiClassName).newInstance();
                }
                catch (Exception e) {
                    //      begin  221334: improve error being logged for this error.
                    String message = JspCoreException.getMsg("jsp.error.failed.load.tei.class", new Object[]{teiClassName});
                    message+=" from "+tldLocation;
                    logger.logp(Level.WARNING, CLASS_NAME, "endElement", message);   //PK27099
                    //throw new SAXException(message);                               //PK27099
                    //      end  221334: improve error being logged for this error.                     
                }
            }
        }
        TagInfo tag = new TagInfo(tagName,
                          tagClassName,
                          bodyContent,
                          tagDescription,
                          tli,
                          tei,
                          tagAttributes,
                          displayName,
                          smallIcon,
                          largeIcon,
                          tagVariables,
                          dynamicAttributes);
        return tag;
    }
}

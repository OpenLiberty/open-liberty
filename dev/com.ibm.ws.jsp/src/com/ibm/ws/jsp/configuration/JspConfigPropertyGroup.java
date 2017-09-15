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
package com.ibm.ws.jsp.configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.descriptor.JspPropertyGroupDescriptor;

import com.ibm.ws.javaee.dd.jsp.JSPPropertyGroup;

public class JspConfigPropertyGroup implements JspPropertyGroupDescriptor {
    /**
     * Comment for <code>serialVersionUID</code>
     */
    private static final long serialVersionUID = 3257289123571511350L;
    protected List<String> urlPatterns = new ArrayList<String>();
    private JSPPropertyGroup propertyGroup;

    //private Collection<String> urlPatterns already exists

    public JspConfigPropertyGroup() {}

    public JspConfigPropertyGroup(JSPPropertyGroup propertyGroup) {
        this.propertyGroup = propertyGroup;
    }

    @Override
    public List<String> getUrlPatterns() {
        return propertyGroup.getURLPatterns();
    }

    @Override
    public String getBuffer() {
        return propertyGroup.getBuffer();
    }

    @Override
    public String getDefaultContentType() {
        return propertyGroup.getDefaultContentType();
    }

    @Override
    public String getDeferredSyntaxAllowedAsLiteral() {
        if(propertyGroup.isSetDeferredSyntaxAllowedAsLiteral()){
            return Boolean.toString(propertyGroup.isDeferredSyntaxAllowedAsLiteral());
        } else {
            return null;
        }
    }

    @Override
    public String getElIgnored() {
        if(propertyGroup.isSetElIgnored()){
            return Boolean.toString(propertyGroup.isElIgnored());
        } else {
            return null;
        }
    }

    @Override
    public String getErrorOnUndeclaredNamespace() {
        if(propertyGroup.isSetErrorOnUndeclaredNamespace()){
            return Boolean.toString(propertyGroup.isErrorOnUndeclaredNamespace());
        } else {
            return null;
        }
    }

    @Override
    public Collection<String> getIncludeCodas() {
        return propertyGroup.getIncludeCodas();
    }

    @Override
    public Collection<String> getIncludePreludes() {
        return propertyGroup.getIncludePreludes();
    }

    @Override
    public String getIsXml() {
        if(propertyGroup.isSetIsXml()){
            return Boolean.toString(propertyGroup.isIsXml());
        } else {
            return null;
        }
    }

    @Override
    public String getPageEncoding() {
        return propertyGroup.getPageEncoding();
    }

    @Override
    public String getScriptingInvalid() {
        if(propertyGroup.isSetScriptingInvalid()){
            return Boolean.toString(propertyGroup.isScriptingInvalid());
        } else {
            return null;
        }
    }

    @Override
    public String getTrimDirectiveWhitespaces() {
        if(propertyGroup.isSetTrimDirectiveWhitespaces()){
            return Boolean.toString(propertyGroup.isTrimDirectiveWhitespaces());
        } else {
            return null;
        }
    }
}

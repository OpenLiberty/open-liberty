/*******************************************************************************
 * Copyright (c) 2011, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 *
 */
public class JspEngineElement extends ConfigElement {
    private Boolean useStringCast;
    private Boolean usescriptvardupinit;
    private String jdkSourceLevel;
    private String javaSourceLevel;
    private Boolean disableResourceInjection;
    private Boolean disableTldSearch;
    private String scratchdir;
    private Boolean keepGenerated;
    private Boolean useJDKCompiler;

    /**
     * @return the useStringCast
     */
    public Boolean getUseStringCast() {
        return useStringCast;
    }

    @XmlAttribute(name = "useStringCast")
    public void setUseStringCast(Boolean b) {
        this.useStringCast = b;
    }

    /**
     * @return the usescriptvardupinit
     */
    public Boolean isUsescriptvardupinit() {
        return usescriptvardupinit;
    }

    @XmlAttribute(name = "useScriptVarDupInit")
    public void setUsescriptvardupinit(Boolean b) {
        this.usescriptvardupinit = b;
    }

    /**
     * @return the jdkSourceLevel
     */
    public String getJdkSourceLevel() {
        return jdkSourceLevel;
    }

    @XmlAttribute(name = "jdkSourceLevel")
    public void setJdkSourceLevel(String s) {
        this.jdkSourceLevel = s;
    }

    /**
     * @return the javaSourceLevel
     */
    public String getJavaSourceLevel() {
        return javaSourceLevel;
    }

    @XmlAttribute(name = "javaSourceLevel")
    public void setJavaSourceLevel(String s) {
        this.javaSourceLevel = s;
    }

    /**
     * @return the disableResourceInjection
     */
    public Boolean isDisableResourceInjection() {
        return disableResourceInjection;
    }

    @XmlAttribute(name = "disableResourceInjection")
    public void setDisableResourceInjection(Boolean b) {
        this.disableResourceInjection = b;
    }

    /**
     * @return the disableTldSearch
     */
    public Boolean isDisableTldSearch() {
        return disableTldSearch;
    }

    @XmlAttribute(name = "disableTldSearch")
    public void setDisableTldSearch(Boolean b) {
        this.disableTldSearch = b;
    }

    @XmlAttribute(name = "scratchdir")
    public void setScratchdir(String value) {
        this.scratchdir = value;

    }

    public String getScratchdir() {
        return scratchdir;
    }

    /**
     * @return the keepGenerated
     */
    public Boolean isKeepGenerated() {
        return keepGenerated;
    }

    @XmlAttribute(name = "keepGenerated")
    public void setKeepGenerated(Boolean b) {
        this.keepGenerated = b;
    }

    /**
     * @return the keepGenerated
     */
    public Boolean isUseJDKCompiler() {
        return useJDKCompiler;
    }

    @XmlAttribute(name = "useJDKCompiler")
    public void setUseJDKCompiler(Boolean b) {
        this.useJDKCompiler = b;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("JspElement{");
        if (useStringCast != null)
            buf.append("useStringCast=\"" + useStringCast + "\" ");
        if (usescriptvardupinit != null)
            buf.append("usescriptvardupinit=\"" + usescriptvardupinit + "\" ");
        if (jdkSourceLevel != null)
            buf.append("jdkSourceLevel=\"" + jdkSourceLevel + "\" ");
        if (javaSourceLevel != null)
            buf.append("javaSourceLevel=\"" + javaSourceLevel + "\" ");
        if (disableResourceInjection != null)
            buf.append("disableResourceInjection=\"" + disableResourceInjection + "\" ");
        if (disableTldSearch != null)
            buf.append("disableTldSearch=\"" + disableTldSearch + "\" ");
        if (scratchdir != null)
            buf.append("scratchdir=\"" + scratchdir + "\" ");
        if (keepGenerated != null)
            buf.append("keepGenerated=\"" + keepGenerated + "\" ");
        if (useJDKCompiler != null)
            buf.append("useJDKCompiler=\"" + useJDKCompiler + "\" ");

        buf.append("}");
        return buf.toString();
    }

}
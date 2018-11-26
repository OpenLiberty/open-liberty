/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.springboot.container.config;

/**
 *
 */
public class JspEngineElement extends ConfigElement {

    public final static String XML_ATTRIBUTE_NAME_USE_STRING_CAST = "useStringCast";
    private Boolean useStringCast;

    public final static String XML_ATTRIBUTE_NAME_USE_SCRIPT_VAR_DUP_INIT = "useScriptVarDupInit";
    private Boolean usescriptvardupinit;

    public final static String XML_ATTRIBUTE_NAME_JDK_SOURCE_LEVEL = "jdkSourceLevel";
    private String jdkSourceLevel;

    public final static String XML_ATTRIBUTE_NAME_DISABLE_RESOURCE_INJECTION = "disableResourceInjection";
    private Boolean disableResourceInjection;

    public final static String XML_ATTRIBUTE_NAME_DISABLE_TLD_SEARCH = "disableTldSearch";
    private Boolean disableTldSearch;

    public final static String XML_ATTRIBUTE_NAME_SCRATCH_DIR = "scratchdir";
    private String scratchdir;

    public final static String XML_ATTRIBUTE_NAME_KEEP_GENERATED = "keepGenerated";
    private Boolean keepGenerated;

    /**
     * @return the useStringCast
     */
    public Boolean getUseStringCast() {
        return useStringCast;
    }

    public void setUseStringCast(Boolean b) {
        this.useStringCast = b;
    }

    /**
     * @return the usescriptvardupinit
     */
    public Boolean isUsescriptvardupinit() {
        return usescriptvardupinit;
    }

    public void setUsescriptvardupinit(Boolean b) {
        this.usescriptvardupinit = b;
    }

    /**
     * @return the jdkSourceLevel
     */
    public String getJdkSourceLevel() {
        return jdkSourceLevel;
    }

    public void setJdkSourceLevel(String s) {
        this.jdkSourceLevel = s;
    }

    /**
     * @return the disableResourceInjection
     */
    public Boolean isDisableResourceInjection() {
        return disableResourceInjection;
    }

    public void setDisableResourceInjection(Boolean b) {
        this.disableResourceInjection = b;
    }

    /**
     * @return the disableTldSearch
     */
    public Boolean isDisableTldSearch() {
        return disableTldSearch;
    }

    public void setDisableTldSearch(Boolean b) {
        this.disableTldSearch = b;
    }

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

    public void setKeepGenerated(Boolean b) {
        this.keepGenerated = b;
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
        if (disableResourceInjection != null)
            buf.append("disableResourceInjection=\"" + disableResourceInjection + "\" ");
        if (disableTldSearch != null)
            buf.append("disableTldSearch=\"" + disableTldSearch + "\" ");
        if (scratchdir != null)
            buf.append("scratchdir=\"" + scratchdir + "\" ");
        if (keepGenerated != null)
            buf.append("keepGenerated=\"" + keepGenerated + "\" ");

        buf.append("}");
        return buf.toString();
    }

}
/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Defines samesite configuration for the httpEndpoint
 *
 */
public class SameSite extends ConfigElement {

    private String lax;
    private String strict;
    private String none;

    /**
     * 
     * @return The Lax values for this entry
     */
    public String getLax() {
        return this.lax;
    }

    /**
     * 
     * @param lax The Lax values for this entry
     */
    @XmlAttribute
    public void setLax(String lax) {
        this.lax = lax;
    }

    /**
     * 
     * @return The Strict values for this entry
     */
    public String getStrict() {
        return this.strict;
    }

    /**
     * 
     * @param strict The Strict values for this entry
     */
    @XmlAttribute
    public void setStrict(String strict) {
        this.strict = strict;
    }

    /**
     * 
     * @return The None values for this entry
     */
    public String getNone() {
        return this.none;
    }

    /**
     * 
     * @param none The None values for this entry
     */
    @XmlAttribute
    public void setNone(String none) {
        this.none = none;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("samesite{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (lax != null)
            buf.append("lax=\"" + lax + "\" ");
        if (strict != null)
            buf.append("strict=\"" + strict + "\" ");
        if (none != null)
            buf.append("none=\"" + none + "\" ");

        buf.append("}");
        return buf.toString();
    }
}

/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.simplicity.config;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * Defines the <headers> configuration for the <httpEndpoint>
 */
public class Headers extends ConfigElement {

    private String add;
    private String set;
    private String setIfMissing;
    private String remove;

    /**
     * @return The headers values for this entry that will be added to responses
     */
    public String getAdd() {
        return add;
    }

    /**
     * @param Sets the header values that will be added to responses
     */
    @XmlAttribute
    public void setAdd(String add) {
        this.add = add;
    }

    /**
     * @return The header values for this entry that will be set to responses
     */
    public String getSet() {
        return set;
    }

    /**
     * @param Sets the header values that will be set to responses
     */
    @XmlAttribute
    public void setSet(String set) {
        this.set = set;
    }

    /**
     * @return The header values for this entry that will be set to responses when missing
     */
    public String getSetIfMissing() {
        return setIfMissing;
    }

    /**
     * @param Sets the header values that will be set to responses when missing
     */
    @XmlAttribute
    public void setSetIfMissing(String setIfMissing) {
        this.setIfMissing = setIfMissing;
    }

    /**
     * @return The header values for this entry that will be removed from responses when present
     */
    public String getRemove() {
        return remove;
    }

    /**
     * @param Sets the header values that will be removed from responses
     */
    @XmlAttribute
    public void setRemove(String remove) {
        this.remove = remove;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer("headers{");
        buf.append("id=\"" + this.getId() + "\" ");
        if (add != null)
            buf.append("add=\"" + add + "\" ");
        if (set != null)
            buf.append("set=\"" + set + "\" ");
        if (setIfMissing != null)
            buf.append("setIfMissing=\"" + setIfMissing + "\" ");
        if (remove != null)
            buf.append("remove=\"" + remove + "\" ");

        buf.append("}");
        return buf.toString();
    }

}

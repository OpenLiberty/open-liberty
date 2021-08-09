/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.metatype;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
public class MetatypeAdOption {
    private String label;
    private String value;
    private String nlsKey;

    public void setNLSKey(String nlsKey) {
        this.nlsKey = nlsKey;
    }

    public String getNLSKey() {
        return nlsKey;
    }

    @XmlAttribute(name = "label")
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        if (this.label == null)
            return this.value;
        else
            return this.label;
    }

    @XmlAttribute(name = "value")
    public void setValue(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("MetatypeAdOption{");
        sb.append("value='").append(value).append("' ");
        sb.append("label='").append(getLabel()).append("' ");
        if (nlsKey != null)
            sb.append("nlsKey='").append(nlsKey).append("' ");
        sb.append("}");

        return sb.toString();
    }

    public String toMetatypeString(int padSpaces) {
        StringBuilder sb = new StringBuilder("<Option ");
        sb.append("value=\"").append(value).append("\" ");
        sb.append("label=\"").append(getLabel()).append("\" ");
        sb.append("/>");

        return sb.toString();
    }
}

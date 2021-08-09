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
package com.ibm.ws.jca.utils.xml.wlp.ra;

import javax.xml.bind.annotation.XmlAttribute;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * wlp-ra.xml option element
 */
@Trivial
public class WlpConfigOption {
    @XmlAttribute(name = "label")
    private String label;
    @XmlAttribute(name = "value")
    private String value;
    @XmlAttribute(name = "nlsKey")
    private String wlp_nlsKey;

    public String getNLSKey() {
        return wlp_nlsKey;
    }

    public String getLabel() {
        if (label == null)
            return value;
        else
            return label;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('{');
        sb.append("value='" + value + "' ");
        sb.append("label='" + getLabel() + "' ");
        if (wlp_nlsKey != null)
            sb.append("nlsKey='" + wlp_nlsKey + "' ");
        sb.append("}");

        return sb.toString();
    }
}

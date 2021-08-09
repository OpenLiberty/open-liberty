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
package com.ibm.ws.jca.utils.xml.ra;

import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jca.utils.xml.ra.v10.Ra10License;

/**
 * ra.xml license element
 */
@Trivial
@XmlType(propOrder = { "description", "licenseRequired" })
public class RaLicense {
    @XmlElement(name = "license-required", required = true)
    private Boolean licenseRequired;
    @XmlElement(name = "description")
    private final List<RaDescription> description = new LinkedList<RaDescription>();
    @XmlID
    @XmlAttribute(name = "id")
    private String id;

    public boolean getLicenseRequired() {
        return this.licenseRequired == null ? false : this.licenseRequired;
    }

    public List<RaDescription> getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("RaLicense{license-required='");
        if (licenseRequired != null)
            sb.append(licenseRequired);
        sb.append("'}");
        return sb.toString();
    }

    public void copyRa10Settings(Ra10License ra10License) {
        if (ra10License.getDescription() != null) {
            RaDescription desc = new RaDescription();
            desc.setValue(ra10License.getDescription());
            description.add(desc);
        }
        licenseRequired = Boolean.parseBoolean(ra10License.getLicenseRequired());
    }
}

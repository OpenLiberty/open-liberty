/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.utils.xml.ra.v10;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
@Trivial
@XmlRootElement(name = "connector")
@XmlType(name = "connectorType", propOrder = { "displayName", "description", "icon", "vendorName", "specVersion", "eisType", "version", "license", "resourceAdapter" })
public class Ra10Connector {

    @XmlElement(name = "display-name", required = true)
    private String displayName;
    @XmlElement(name = "description")
    private String description;
    @XmlElement(name = "icon")
    private Ra10Icon icon;
    @XmlElement(name = "vendor-name", required = true)
    private String vendorName;
    @XmlElement(name = "spec-version", required = true)
    private String specVersion;
    @XmlElement(name = "eis-type", required = true)
    private String eisType;
    @XmlElement(name = "version", required = true)
    private String version;
    @XmlElement(name = "license")
    private Ra10License license;
    @XmlElement(name = "resourceadapter", required = true)
    private Ra10ResourceAdapter resourceAdapter;

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the icon
     */
    public Ra10Icon getIcon() {
        return icon;
    }

    /**
     * @return the vendorName
     */
    public String getVendorName() {
        return vendorName;
    }

    /**
     * @return the specVersion
     */
    public String getSpecVersion() {
        return specVersion;
    }

    /**
     * @return the eisType
     */
    public String getEisType() {
        return eisType;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @return the license
     */
    public Ra10License getLicense() {
        return license;
    }

    /**
     * @return the resourceAdapter
     */
    public Ra10ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

}

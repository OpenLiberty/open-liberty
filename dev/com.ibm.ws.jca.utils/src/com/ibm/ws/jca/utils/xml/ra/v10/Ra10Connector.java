/*******************************************************************************
 * Copyright (c) 2013,2022 IBM Corporation and others.
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
package com.ibm.ws.jca.utils.xml.ra.v10;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.ibm.websphere.ras.annotation.Trivial;

@Trivial
@XmlRootElement(name = "connector")
@XmlType(name = "connectorType",
         propOrder = { "displayName", "description", "icon",
                       "vendorName", "specVersion", "eisType",
                       "version", "license", "resourceAdapter" })
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

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public Ra10Icon getIcon() {
        return icon;
    }

    public String getVendorName() {
        return vendorName;
    }

    public String getSpecVersion() {
        return specVersion;
    }

    public String getEisType() {
        return eisType;
    }

    public String getVersion() {
        return version;
    }

    public Ra10License getLicense() {
        return license;
    }

    public Ra10ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }
}

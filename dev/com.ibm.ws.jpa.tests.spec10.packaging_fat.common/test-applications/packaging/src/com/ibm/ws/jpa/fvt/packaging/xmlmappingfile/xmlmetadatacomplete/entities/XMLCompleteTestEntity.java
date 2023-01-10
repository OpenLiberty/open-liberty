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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.xmlmetadatacomplete.entities;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class XMLCompleteTestEntity {
    @Id
    private int id;

    @Basic(optional = false)
    @Column(name = "BogusShouldBeIgnoredByXMLMetadataComplete")
    private String nonOptionalName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNonOptionalName() {
        return nonOptionalName;
    }

    public void setNonOptionalName(String nonOptionalName) {
        this.nonOptionalName = nonOptionalName;
    }

    @Override
    public String toString() {
        return "XMLCompleteTestEntity [id=" + id + ", nonOptionalName=" + nonOptionalName + "]";
    }
}

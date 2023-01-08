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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.annotationoverride.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Entity supplied with table schema overriding annotations, which are to be overriden by XML Mapping File settings.
 * DEVELOPER: Ensure that the DDL contains the correct overriden table structure.
 *
 *
 */
@Entity
@Table(name = "AnnTableName", /* catalog = "AnnCatName", */ schema = "AnnSchName")
public class TableSchemaOverrideEntity {
    @Id
    private int id;

    @Column(name = "AnnName")
    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TableSchemaOverrideEntity [id=" + id + ", name=" + name + "]";
    }

}

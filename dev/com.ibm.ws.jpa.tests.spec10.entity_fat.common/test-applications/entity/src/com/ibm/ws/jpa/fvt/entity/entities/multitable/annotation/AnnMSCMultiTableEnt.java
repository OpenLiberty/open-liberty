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

package com.ibm.ws.jpa.fvt.entity.entities.multitable.annotation;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.SecondaryTable;

@Entity
@SecondaryTable(name = "SEC_TABLE2AMSC", pkJoinColumns = @PrimaryKeyJoinColumn(name = "id"))
@AttributeOverrides({ @AttributeOverride(name = "street", column = @Column(table = "SEC_TABLE2AMSC")),
                      @AttributeOverride(name = "city", column = @Column(table = "SEC_TABLE2AMSC")),
                      @AttributeOverride(name = "state", column = @Column(table = "SEC_TABLE2AMSC")),
                      @AttributeOverride(name = "zip", column = @Column(table = "SEC_TABLE2AMSC")) })
public class AnnMSCMultiTableEnt extends AnnMSCMultiTable {
    @Id
    private int id;

    private String name;

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "AnnMSCMultiTableEnt [id=" + id + "]";
    }
}

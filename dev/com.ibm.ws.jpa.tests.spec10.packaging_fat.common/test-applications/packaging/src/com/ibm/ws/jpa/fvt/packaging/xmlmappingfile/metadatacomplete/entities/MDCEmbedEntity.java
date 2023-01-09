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

package com.ibm.ws.jpa.fvt.packaging.xmlmappingfile.metadatacomplete.entities;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MDCEmbedEntity implements IMDCEntity {
    @Id
    private int id;

    @Embedded
    private MDCEmbeddable embeddable;

    public MDCEmbedEntity() {
        embeddable = new MDCEmbeddable();
    }

    public MDCEmbeddable getEmbeddable() {
        return embeddable;
    }

    public void setEmbeddable(MDCEmbeddable embeddable) {
        this.embeddable = embeddable;
    }

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
        return embeddable.getName();
    }

    @Override
    public void setName(String name) {
        embeddable.setName(name);
    }

    @Override
    public String toString() {
        return "MDCEmbedEntity [id=" + id + ", embeddable=" + embeddable + "]";
    }
}

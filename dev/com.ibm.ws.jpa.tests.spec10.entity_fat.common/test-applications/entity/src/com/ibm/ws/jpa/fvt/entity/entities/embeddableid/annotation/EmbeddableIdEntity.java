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

package com.ibm.ws.jpa.fvt.entity.entities.embeddableid.annotation;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import com.ibm.ws.jpa.fvt.entity.entities.IEmbeddableIdEntity;
import com.ibm.ws.jpa.fvt.entity.entities.embeddableid.EmbeddableIdObject;

@Entity
public class EmbeddableIdEntity implements IEmbeddableIdEntity {
    @EmbeddedId
    private EmbeddableIdObject pkey;

    private int intVal;

    public EmbeddableIdEntity() {
        pkey = new EmbeddableIdObject();
    }

    public EmbeddableIdEntity(int id, String country) {
        this.pkey = new EmbeddableIdObject(id, country);
    }

    @Override
    public int getIntVal() {
        return intVal;
    }

    @Override
    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    @Override
    public EmbeddableIdObject getPkey() {
        return pkey;
    }

    public void setPkey(EmbeddableIdObject newKey) {
        pkey = newKey;
    }
}

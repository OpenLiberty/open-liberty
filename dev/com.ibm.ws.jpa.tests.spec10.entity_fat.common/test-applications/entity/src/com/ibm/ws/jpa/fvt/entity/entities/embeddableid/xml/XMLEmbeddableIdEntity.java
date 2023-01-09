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

package com.ibm.ws.jpa.fvt.entity.entities.embeddableid.xml;

import com.ibm.ws.jpa.fvt.entity.entities.IEmbeddableIdEntity;
import com.ibm.ws.jpa.fvt.entity.entities.embeddableid.EmbeddableIdObject;

public class XMLEmbeddableIdEntity implements IEmbeddableIdEntity {
    private EmbeddableIdObject pkey;

    private int intVal;

    public XMLEmbeddableIdEntity() {
        pkey = new EmbeddableIdObject();
    }

    public XMLEmbeddableIdEntity(int id, String country) {
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

/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package com.ibm.ws.jpa.olgh16686.model;

import java.util.Date;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;
import javax.persistence.MapKeyTemporal;
import javax.persistence.TemporalType;

@Entity
public class ElemCollEntityOLGH16686 {

    @Id
    private int id;

    /*
     * TODO: JPA Spec defines that the default CollectionTable JoinColumn name is the concatenation of the following:
     * the name of the entity; "_"; the name of the referenced primary key column
     *
     * OpenJPA defaults to expect "ELEME_ID"
     * EclipseLink defaults to expect "ELEMENTCOLLECTIONENTITYOLGH16686_ID"
     *
     * Since there is a difference, we need to define the JoinColumn for now to force OpenJPA
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "EntMapDateTemporal")
    @MapKeyColumn(name = "mykey")
    @MapKeyTemporal(TemporalType.DATE)
    private Map<Date, ElemCollEmbedTemporalOLGH16686> mapKeyTemporalValueEmbed;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<Date, ElemCollEmbedTemporalOLGH16686> getMapKeyTemporalValueEmbed() {
        return mapKeyTemporalValueEmbed;
    }

    public void setMapKeyTemporalValueEmbed(Map<Date, ElemCollEmbedTemporalOLGH16686> mapKeyTemporalValueEmbed) {
        this.mapKeyTemporalValueEmbed = mapKeyTemporalValueEmbed;
    }
}

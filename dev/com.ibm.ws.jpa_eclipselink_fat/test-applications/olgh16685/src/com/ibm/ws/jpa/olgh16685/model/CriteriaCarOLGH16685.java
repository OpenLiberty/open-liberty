/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jpa.olgh16685.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Entity
@IdClass(value = CriteriaCarIdOLGH16685.class)
public class CriteriaCarOLGH16685 {

    @Id
    @Column(name = "CAR_ID")
    private String id;

    @Id
    @Column(name = "CAR_VER")
    private int version;

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "component")
    @Column(name = "origin", updatable = true, insertable = true)
    @CollectionTable(name = "criteria_car_origin",
                     joinColumns = {
                                     @JoinColumn(name = "CAR_ID", referencedColumnName = "CAR_ID"),
                                     @JoinColumn(name = "CAR_VER", referencedColumnName = "CAR_VER") })
    private Map<String, String> origin = new HashMap<String, String>();

    public CriteriaCarOLGH16685() {
    }

    public CriteriaCarOLGH16685(String id, int version, Map<String, String> origin) {
        this.id = id;
        this.version = version;
        this.origin = origin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Map<String, String> getOrigin() {
        return origin;
    }

    public void setOrigin(Map<String, String> origin) {
        this.origin = origin;
    }
}

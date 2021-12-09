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
package com.ibm.ws.jpa.olgh19182.model;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Basic;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;

@Entity
public class HelmetEntityOLGH19182 {

    @Id
    protected int id;

    @Basic(fetch = FetchType.LAZY)
    protected String color;

    @ElementCollection
//    @PrivateOwned
    @CollectionTable(
                     name = "JPA_HELMET_PROPERTIES",
                     joinColumns = {
                                     @JoinColumn(name = "HELMET_ID", referencedColumnName = "ID")
                     })
    @Column(name = "PROPERTY_VALUE")
    @MapKeyColumn(name = "PROPERTY_NAME")
    protected Map<String, String> properties;

    @ManyToOne(targetEntity = ShelfEntityOLGH19182.class)
    @JoinColumn(name = "SHELF_ID", referencedColumnName = "ID")
    private ShelfEntityOLGH19182 shelf;

    public HelmetEntityOLGH19182() {
        super();
        this.properties = new HashMap<String, String>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public void addProperty(String propertyName, String propertyValue) {
        getProperties().put(propertyName, propertyValue);
    }

    public void removeProperty(String propertyName) {
        getProperties().remove(propertyName);
    }

    public void setShelf(ShelfEntityOLGH19182 shelf) {
        this.shelf = shelf;
    }

    public ShelfEntityOLGH19182 getShelf() {
        return this.shelf;
    }
}

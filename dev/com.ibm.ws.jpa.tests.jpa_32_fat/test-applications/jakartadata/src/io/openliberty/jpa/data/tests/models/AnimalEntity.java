/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;


public class AnimalEntity {

    private Animal.ScientificName id;
    private String commonName;
    private long version;

    public AnimalEntity() { }

    /** 
     * Convenience constructor: build the entity from your Record 
     */
    public AnimalEntity(Animal rec) {
        this.id         = rec.id();
        this.commonName = rec.commonName();
        this.version    = rec.version();
    }

    public Animal.ScientificName getId() {
        return id;
    }

    public void setId(Animal.ScientificName id) {
        this.id = id;
    }

    public String getCommonName() {
        return commonName;
    }

    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    /**
     * Convert back to the Record type
     */
    public Animal toRecord() {
        return new Animal(id, commonName, version);
    }
}


/*******************************************************************************
 * Copyright (c) 2022,2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Version;

/**
 * This entity has a ManyToOne relationship with Manufacturer.
 */
@Entity
public class Model {

    @Id
    private UUID id;

    @JoinColumn(name = "manufacturer_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Manufacturer manufacturer;

    @Column(name = "name")
    private String name;

    @Column(name = "updated_at")
    @Version
    Instant updatedAt;

    @Column(name = "intro_year")
    private Integer yearIntroduced;

    public Model() {
        id = UUID.randomUUID();
    }

    public UUID getId() {
        return id;
    }

    public Manufacturer getManufacturer() {
        return manufacturer;
    }

    public String getName() {
        return name;
    }

    public Integer getYearIntroduced() {
        return yearIntroduced;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setManufacturer(Manufacturer manufacturer) {
        this.manufacturer = manufacturer;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setYearIntroduced(Integer yearIntroduced) {
        this.yearIntroduced = yearIntroduced;
    }

    @Override
    public String toString() {
        return "Model " + name + " " + yearIntroduced + " " + id;
    }
}

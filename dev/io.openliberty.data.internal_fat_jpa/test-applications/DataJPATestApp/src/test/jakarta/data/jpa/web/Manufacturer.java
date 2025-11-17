/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.jpa.web;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * This entity has a OneToMany relationship with Model.
 */
@Entity
public class Manufacturer {

    @Id
    private UUID id;

    @OneToMany(cascade = CascadeType.ALL,
               mappedBy = "manufacturer",
               fetch = FetchType.EAGER)
    private Set<Model> models;

    private String name;

    private String notes;

    public Manufacturer() {
        id = UUID.randomUUID();
        models = new LinkedHashSet<>();
    }

    public void addModel(Model model) {
        models.add(model);
        model.setManufacturer(this);
    }

    public UUID getId() {
        return id;
    }

    public Set<Model> getModels() {
        return models;
    }

    public String getName() {
        return name;
    }

    public String getNotes() {
        return notes;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public void setModels(Set<Model> models) {
        for (Model model : this.models) {
            model.setManufacturer(null);
        }

        this.models.clear();

        for (Model model : models) {
            addModel(model);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Manufacturer " + name + " " + id + ": " + notes;
    }
}

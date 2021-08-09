/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.fat.jsonb;

import java.time.LocalDate;

public class Widget {

    private String name;
    private LocalDate creationDate;
    private int quantity;
    private double weight;
    private Person owner;

    public Widget() {
    }

    public Widget(String name, LocalDate creationDate, int quantity, double weight, Person owner) {
        this.name = name;
        this.creationDate = creationDate;
        this.quantity = quantity;
        this.weight = weight;
        this.owner = owner;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    /**
     * @return the creationDate
     */
    public LocalDate getCreationDate() {
        return creationDate;
    }
    /**
     * @param creationDate the creationDate to set
     */
    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }
    /**
     * @return the quantity
     */
    public int getQuantity() {
        return quantity;
    }
    /**
     * @param quantity the quantity to set
     */
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    /**
     * @return the weight
     */
    public double getWeight() {
        return weight;
    }
    /**
     * @param weight the weight to set
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }
    /**
     * @return the owner
     */
    public Person getOwner() {
        return owner;
    }
    /**
     * @param owner the owner to set
     */
    public void setOwner(Person owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return name + " " + creationDate + " " + quantity + " " + weight + " " + owner;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Widget)) {
            return false;
        }
        Widget w = (Widget) o;
        return name.equals(w.name) &&
               creationDate.equals(w.creationDate) &&
               quantity == w.quantity &&
               weight == w.weight &&
               owner.equals(w.owner);
    }
}

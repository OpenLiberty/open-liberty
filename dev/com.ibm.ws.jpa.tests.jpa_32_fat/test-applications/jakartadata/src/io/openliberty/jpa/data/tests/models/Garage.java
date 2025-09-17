/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.data.tests.models;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Garage {

    public static enum Type {
        Attached, Detached, TuckUnder
    };

    @Column(name = "GARAGE_AREA")
    private double area;

    @Column(name = "GARAGE_TYPE")
    private Type type;

    //@Embedded
    private Door door;

    // Getters and Setters
    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Door getDoor() {
        return door;
    }

    public void setDoor(Door door) {
        this.door = door;
    }
}

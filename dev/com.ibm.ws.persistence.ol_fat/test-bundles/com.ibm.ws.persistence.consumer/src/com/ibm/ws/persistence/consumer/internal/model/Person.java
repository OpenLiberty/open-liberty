/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.persistence.consumer.internal.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Version;

@Entity
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id;

    @Version
    int version;

    String data;

    Serializable serializableField;

    // @Transient
    // Note -- overridden in orm.xml!
    private String firstName, lastName;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<Car> cars;

    public Person() {

    }

    public Person(String f, String l, String d) {
        firstName = f;
        lastName = l;
        data = d;
    }

    public long getId() {
        return id;
    }

    /**
     * @return the first
     */
    public String getFirst() {
        return firstName;
    }

    /**
     * @param first
     *            the first to set
     */
    public void setFirst(String first) {
        this.firstName = first;
    }

    /**
     * @return the last
     */
    public String getLast() {
        return lastName;
    }

    /**
     * @param last
     *            the last to set
     */
    public void setLast(String last) {
        this.lastName = last;
    }

    public void addCar(Car car) {
        if (cars == null) {
            cars = new ArrayList<Car>();
        }
        cars.add(car);
        if (car.getOwner() != this) {
            car.setOwner(this);
        }
    }

    public List<Car> getCars() {
        return cars;
    }

    /**
     * @return the serializeableField
     */
    public Serializable getSerializable() {
        return serializableField;
    }

    /**
     * @param serializeableField
     *            the serializeableField to set
     */
    public void setSerializableField(Serializable serializeableField) {
        this.serializableField = serializeableField;
    }

}

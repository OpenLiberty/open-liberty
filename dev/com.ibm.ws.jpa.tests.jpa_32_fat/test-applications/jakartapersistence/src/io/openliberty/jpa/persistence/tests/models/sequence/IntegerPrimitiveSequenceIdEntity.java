/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models.sequence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class IntegerPrimitiveSequenceIdEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id; // GenerationType.SEQUENCE with 'int' primary key

    private String name;

    public static IntegerPrimitiveSequenceIdEntity of(String name) {
        IntegerPrimitiveSequenceIdEntity integerPrimitiveSequenceIdEntity = new IntegerPrimitiveSequenceIdEntity();
        integerPrimitiveSequenceIdEntity.setName(name);
        return integerPrimitiveSequenceIdEntity;
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
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

}

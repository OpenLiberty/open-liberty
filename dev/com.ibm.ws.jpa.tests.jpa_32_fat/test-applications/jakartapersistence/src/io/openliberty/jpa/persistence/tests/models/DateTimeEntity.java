/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.jpa.persistence.tests.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class DateTimeEntity {
    @Id
    private int id;
    private String name;
    private java.time.LocalDate localDateData;
    private java.time.LocalTime localTimeData;
    private java.time.LocalDateTime localDateTimeData;

    public DateTimeEntity() {

    }

    public DateTimeEntity(int id) {
        this.id = id;
    }

    /**
     * @param id
     * @param localDateData
     * @param localTimeData
     * @param localDateTimeData
     */
    public DateTimeEntity(int id, String name, LocalDate localDateData, LocalTime localTimeData, LocalDateTime localDateTimeData) {
        super();
        this.id = id;
        this.name = name;
        this.localDateData = localDateData;
        this.localTimeData = localTimeData;
        this.localDateTimeData = localDateTimeData;
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
     * @return the localDateData
     */
    public java.time.LocalDate getLocalDateData() {
        return localDateData;
    }

    /**
     * @param localDateData the localDateData to set
     */
    public void setLocalDateData(java.time.LocalDate localDateData) {
        this.localDateData = localDateData;
    }

    /**
     * @return the localTimeData
     */
    public java.time.LocalTime getLocalTimeData() {
        return localTimeData;
    }

    /**
     * @param localTimeData the localTimeData to set
     */
    public void setLocalTimeData(java.time.LocalTime localTimeData) {
        this.localTimeData = localTimeData;
    }

    /**
     * @return the localDateTimeData
     */
    public java.time.LocalDateTime getLocalDateTimeData() {
        return localDateTimeData;
    }

    /**
     * @param localDateTimeData the localDateTimeData to set
     */
    public void setLocalDateTimeData(java.time.LocalDateTime localDateTimeData) {
        this.localDateTimeData = localDateTimeData;
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

    @Override
    public String toString() {
        return "QueryDateTimeEntity <<<id=" + id + ", localDateData=" + localDateData + ", localTimeData=" + localTimeData + ", localDateTimeData=" + localDateTimeData + ">>>";
    }

}
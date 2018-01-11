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

package jpa22timeapi.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "TimeAPI_ENT")
public class TimeAPIEntity {
    @Id
    @GeneratedValue
    private long id;

    @Basic
    private java.time.LocalDate localDate;
    @Basic
    private java.time.LocalDateTime localDateTime;
    @Basic
    private java.time.LocalTime localTime;
    @Basic
    private java.time.OffsetTime offsetTime;
    @Basic
    private java.time.OffsetDateTime offsetDateTime;

    @Version
    private long version;

    public TimeAPIEntity() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public java.time.LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(java.time.LocalDate localDate) {
        this.localDate = localDate;
    }

    public java.time.LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(java.time.LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public java.time.LocalTime getLocalTime() {
        return localTime;
    }

    public void setLocalTime(java.time.LocalTime localTime) {
        this.localTime = localTime;
    }

    public java.time.OffsetTime getOffsetTime() {
        return offsetTime;
    }

    public void setOffsetTime(java.time.OffsetTime offsetTime) {
        this.offsetTime = offsetTime;
    }

    public java.time.OffsetDateTime getOffsetDateTime() {
        return offsetDateTime;
    }

    public void setOffsetDateTime(java.time.OffsetDateTime offsetDateTime) {
        this.offsetDateTime = offsetDateTime;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "TimeAPIEntity [id=" + id + ", localDate=" + localDate + ", localDateTime=" + localDateTime
               + ", localTime=" + localTime + ", offsetTime=" + offsetTime + ", offsetDateTime=" + offsetDateTime
               + ", version=" + version + "]";
    }

}

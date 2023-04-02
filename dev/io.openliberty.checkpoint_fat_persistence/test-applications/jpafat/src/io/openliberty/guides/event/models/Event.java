// tag::copyright[]
/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - Initial implementation
 *******************************************************************************/
// end::copyright[]
package io.openliberty.guides.event.models;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

// tag::Entity[]
@Entity
// end::Entity[]
// tag::Table[]
@Table(name = "Event")
// end::Table[]
// tag::NamedQuery[]
@NamedQuery(name = "Event.findAll", query = "SELECT e FROM Event e")
@NamedQuery(name = "Event.findEvent", query = "SELECT e FROM Event e WHERE "
                                              + "e.name = :name AND e.location = :location AND e.time = :time")
// end::NamedQuery[]
// tag::Event[]
public class Event implements Serializable {
    private static final long serialVersionUID = 1L;

    // tag::GeneratedValue[]
    @GeneratedValue(strategy = GenerationType.AUTO)
    // end::GeneratedValue[]
    // tag::Id[]
    @Id
    // end::Id[]
    // tag::Column[]
    @Column(name = "eventId")
    // end::Column[]
    private int id;

    @Column(name = "eventLocation")
    private String location;
    @Column(name = "eventTime")
    private String time;
    @Column(name = "eventName")
    private String name;

    public Event() {
    }

    public Event(String name, String location, String time) {
        this.name = name;
        this.location = location;
        this.time = time;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
                 + (int) (serialVersionUID ^ (serialVersionUID >>> 32));
        result = prime * result + ((time == null) ? 0 : time.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Event other = (Event) obj;
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        if (time == null) {
            if (other.time != null) {
                return false;
            }
        } else if (!time.equals(other.time)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return "Event [name=" + name + ", location=" + location + ", time=" + time
               + "]";
    }
}
// end::Event[]

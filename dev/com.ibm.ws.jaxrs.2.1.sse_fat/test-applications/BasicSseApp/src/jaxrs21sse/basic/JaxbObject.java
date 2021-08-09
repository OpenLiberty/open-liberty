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
/**
 *
 */
package jaxrs21sse.basic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JaxbObject {

    static JaxbObject[] JAXB_OBJECTS = new JaxbObject[] {
                                                          new JaxbObject(7, "shiny", 3.14, new Date()),
                                                          new JaxbObject(Long.MAX_VALUE, "big", Double.MAX_VALUE, new Date(new Date().getTime() + 10000)),
                                                          new JaxbObject(Long.MIN_VALUE, "small", Double.MIN_VALUE, new Date(new Date().getTime() - 100000))
    };

    long id;
    String description;
    double cost;
    Date timeStamp;

    public JaxbObject() {}

    public JaxbObject(long id, String description, double cost, Date timeStamp) {
        this.id = id;
        this.description = description;
        this.cost = cost;
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JaxbObject) {
            JaxbObject other = (JaxbObject) o;
            Instant thisInstant = this.timeStamp.toInstant();
            Instant otherInstant = other.timeStamp.toInstant();
            return (thisInstant.isAfter(otherInstant.minus(10, ChronoUnit.SECONDS)) &&
                    thisInstant.isBefore(otherInstant.plus(10, ChronoUnit.SECONDS)) &&
                    this.cost == other.cost &&
                    this.description.equals(other.description) &&
                    this.id == other.id);
        }
        return false;
    }

    public long getLongID() {
        return id;
    }

    @XmlAttribute
    public void setLongID(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    @XmlElement
    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    @XmlElement
    public void setCost(double cost) {
        this.cost = cost;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    @XmlElement
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}

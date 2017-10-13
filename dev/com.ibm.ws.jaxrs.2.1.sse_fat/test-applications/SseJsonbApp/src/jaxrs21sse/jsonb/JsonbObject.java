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
package jaxrs21sse.jsonb;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({ "description", "id", "cost", "timeStamp" })
public class JsonbObject {

    static JsonbObject[] JSONB_OBJECTS = new JsonbObject[] {
                                                             new JsonbObject(7, "shiny", 3.14, new Date()),
                                                             new JsonbObject(Long.MAX_VALUE, "big", Double.MAX_VALUE, new Date(new Date().getTime() + 10000)),
                                                             new JsonbObject(Long.MIN_VALUE, "small", Double.MIN_VALUE, new Date(new Date().getTime() - 100000))
    };

    @JsonbProperty("Z")
    long id;
    @JsonbProperty("M")
    String description;
    @JsonbProperty("Y")
    double cost;
    @JsonbProperty("B")
    Date timeStamp;

    public JsonbObject() {}

    public JsonbObject(long id, String description, double cost, Date timeStamp) {
        this.id = id;
        this.description = description;
        this.cost = cost;
        this.timeStamp = timeStamp;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof JsonbObject) {
            JsonbObject other = (JsonbObject) o;
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

    public boolean confirmOrder(String s) {
        String deliminator = ",";
        String[] properties = s.split(deliminator);
        int i = 0;
        if (properties.length == 4) {
            return (properties[0].startsWith("{\"M") &&
                    properties[1].startsWith("\"Z") &&
                    properties[2].startsWith("\"Y") &&
                    properties[3].startsWith("\"B"));
        }
        return false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }
}

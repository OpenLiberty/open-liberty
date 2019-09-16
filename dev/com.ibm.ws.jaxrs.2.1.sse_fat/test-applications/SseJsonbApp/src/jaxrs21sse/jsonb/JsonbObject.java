/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbPropertyOrder;

@JsonbPropertyOrder({ "id", "cost", "timeStamp", "description" })
public class JsonbObject {
    static final String[] EXPECTED_ORDER = { "identity", "thecost", "tstamp", "desc" };
    
    static JsonbObject[] JSONB_OBJECTS = new JsonbObject[] {
                                                             new JsonbObject(7, "shiny", 3.14, new Date()),
                                                             new JsonbObject(Long.MAX_VALUE, "big", Double.MAX_VALUE, new Date(new Date().getTime() + 10000)),
                                                             new JsonbObject(Long.MIN_VALUE, "small", Double.MIN_VALUE, new Date(new Date().getTime() - 100000))
    };

    @JsonbProperty("identity")
    long id;
    @JsonbProperty("desc")
    String description;
    @JsonbProperty("thecost")
    double cost;
    @JsonbProperty("tstamp")
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

    public static void confirmOrder(String s) {
        String[] properties = s.split(",");
        assertEquals("Expected exactly 4 JSON properties but got: " + s, 4, properties.length);
        for (int i = 0; i < 4; i++)
            assertTrue("Expected JSON attributes in the order " + Arrays.toString(EXPECTED_ORDER) + " but got: " + s,
                         properties[i].contains('"' + EXPECTED_ORDER[i]));
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

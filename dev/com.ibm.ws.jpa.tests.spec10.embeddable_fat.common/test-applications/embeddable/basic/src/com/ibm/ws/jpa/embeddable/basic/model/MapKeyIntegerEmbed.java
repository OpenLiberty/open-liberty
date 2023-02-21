/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;

@Embeddable
public class MapKeyIntegerEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final Map<Integer, Integer> INIT;
    static {
        Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        map.put(new Integer(3), new Integer(300));
        map.put(new Integer(1), new Integer(100));
        map.put(new Integer(2), new Integer(200));
        INIT = Collections.unmodifiableMap(map);
    }

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "MapIntInt", joinColumns = @JoinColumn(name = "parent_id"))
    @MapKeyColumn(name = "mykey")
    @Column(name = "value")
    private Map<Integer, Integer> notMapKeyInteger;

    public MapKeyIntegerEmbed() {
    }

    public MapKeyIntegerEmbed(Map<Integer, Integer> notMapKeyInteger) {
        this.notMapKeyInteger = notMapKeyInteger;
    }

    public Map<Integer, Integer> getNotMapKeyInteger() {
        return this.notMapKeyInteger;
    }

    public void setNotMapKeyInteger(Map<Integer, Integer> notMapKeyInteger) {
        this.notMapKeyInteger = notMapKeyInteger;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof MapKeyIntegerEmbed))
            return false;
        return (((MapKeyIntegerEmbed) otherObject).notMapKeyInteger.equals(notMapKeyInteger)); // Can't use hash b/c not sorted.
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (notMapKeyInteger != null) {
            // EclipseLink wraps order column Lists in an IndirectMap implementation and overrides toString()
            Map<Integer, Integer> temp = new HashMap<Integer, Integer>(notMapKeyInteger);
            sb.append("notMapKeyInteger=" + temp.toString());
        } else {
            sb.append("notMapKeyInteger=null");
        }
        return sb.toString();
    }
}

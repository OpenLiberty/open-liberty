/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.fvt.jpa20.querylockmode.model;

import java.io.Serializable;

/**
 * <p>Id class of the Common Datamodel (which uses all the possible JPA 2.0 Annotations as described in the
 * <a href="http://www.j2ee.me/javaee/6/docs/api/javax/persistence/package-summary.html">javax.persistence documentation</a>)
 *
 *
 * <p><b>Notes:</b>
 * <ol>
 * <li>Per the JSR-317 spec (page 28), the primary key class:
 * <ul>
 * <li>Must be serializable
 * <li>Must define equals and hashCode methods
 * </ul>
 * </ol>
 */
public class IdClass0310 implements Serializable {

    private int entity0310_id1;

    private Integer entity0310_id2;

    private long entity0310_id3;

    public IdClass0310() {}

    public IdClass0310(int id1,
                       Integer id2,
                       long id3) {
        this.entity0310_id1 = id1;
        this.entity0310_id2 = id2;
        this.entity0310_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0310: " +
                " entity0310_id1: " + getEntity0310_id1() +
                " entity0310_id2: " + getEntity0310_id2() +
                " entity0310_id3: " + getEntity0310_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0310))
            return false;
        if (o == this)
            return true;
        IdClass0310 idClass = (IdClass0310) o;
        return (idClass.entity0310_id1 == entity0310_id1 &&
                idClass.entity0310_id2 == entity0310_id2 &&
                idClass.entity0310_id3 == entity0310_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0310_id1;
        result = result + entity0310_id2.hashCode();
        result = result + (int) (entity0310_id3 ^ (entity0310_id3 >>> 32));
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntity0310_id1() {
        return entity0310_id1;
    }

    public Integer getEntity0310_id2() {
        return entity0310_id2;
    }

    public long getEntity0310_id3() {
        return entity0310_id3;
    }
}

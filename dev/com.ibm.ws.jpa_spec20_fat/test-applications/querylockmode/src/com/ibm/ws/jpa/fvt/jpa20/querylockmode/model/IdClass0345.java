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
public class IdClass0345 implements Serializable {

    private Float entity0345_id1;

    private long entity0345_id2;

    private Short entity0345_id3;

    public IdClass0345() {}

    public IdClass0345(Float id1,
                       long id2,
                       Short id3) {
        this.entity0345_id1 = id1;
        this.entity0345_id2 = id2;
        this.entity0345_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0345: " +
                " entity0345_id1: " + getEntity0345_id1() +
                " entity0345_id2: " + getEntity0345_id2() +
                " entity0345_id3: " + getEntity0345_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0345))
            return false;
        if (o == this)
            return true;
        IdClass0345 idClass = (IdClass0345) o;
        return (idClass.entity0345_id1 == entity0345_id1 &&
                idClass.entity0345_id2 == entity0345_id2 &&
                idClass.entity0345_id3 == entity0345_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0345_id1.hashCode();
        result = result + (int) (entity0345_id2 ^ (entity0345_id2 >>> 32));
        result = result + entity0345_id3.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Float getEntity0345_id1() {
        return entity0345_id1;
    }

    public long getEntity0345_id2() {
        return entity0345_id2;
    }

    public Short getEntity0345_id3() {
        return entity0345_id3;
    }
}

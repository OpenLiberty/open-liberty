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
public class IdClass0323 implements Serializable {

    private Character entity0323_id1;

    private double entity0323_id2;

    private float entity0323_id3;

    public IdClass0323() {}

    public IdClass0323(Character id1,
                       double id2,
                       float id3) {
        this.entity0323_id1 = id1;
        this.entity0323_id2 = id2;
        this.entity0323_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0323: " +
                " entity0323_id1: " + getEntity0323_id1() +
                " entity0323_id2: " + getEntity0323_id2() +
                " entity0323_id3: " + getEntity0323_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0323))
            return false;
        if (o == this)
            return true;
        IdClass0323 idClass = (IdClass0323) o;
        return (idClass.entity0323_id1 == entity0323_id1 &&
                idClass.entity0323_id2 == entity0323_id2 &&
                idClass.entity0323_id3 == entity0323_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0323_id1.hashCode();
        Long long2 = Double.doubleToLongBits(entity0323_id2);
        result = result + (int) (long2 ^ (long2 >>> 32));
        result = result + Float.floatToIntBits(entity0323_id3);
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Character getEntity0323_id1() {
        return entity0323_id1;
    }

    public double getEntity0323_id2() {
        return entity0323_id2;
    }

    public float getEntity0323_id3() {
        return entity0323_id3;
    }
}

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

package suite.r80.base.common.datamodel.entities;

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
public class IdClass0325 implements Serializable {

    private double entity0325_id1;

    private float entity0325_id2;

    private int entity0325_id3;

    public IdClass0325() {}

    public IdClass0325(double id1,
                       float id2,
                       int id3) {
        this.entity0325_id1 = id1;
        this.entity0325_id2 = id2;
        this.entity0325_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0325: " +
                " entity0325_id1: " + getEntity0325_id1() +
                " entity0325_id2: " + getEntity0325_id2() +
                " entity0325_id3: " + getEntity0325_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0325))
            return false;
        if (o == this)
            return true;
        IdClass0325 idClass = (IdClass0325) o;
        return (idClass.entity0325_id1 == entity0325_id1 &&
                idClass.entity0325_id2 == entity0325_id2 &&
                idClass.entity0325_id3 == entity0325_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        Long long1 = Double.doubleToLongBits(entity0325_id1);
        result = result + (int) (long1 ^ (long1 >>> 32));
        result = result + Float.floatToIntBits(entity0325_id2);
        result = result + entity0325_id3;
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public double getEntity0325_id1() {
        return entity0325_id1;
    }

    public float getEntity0325_id2() {
        return entity0325_id2;
    }

    public int getEntity0325_id3() {
        return entity0325_id3;
    }
}

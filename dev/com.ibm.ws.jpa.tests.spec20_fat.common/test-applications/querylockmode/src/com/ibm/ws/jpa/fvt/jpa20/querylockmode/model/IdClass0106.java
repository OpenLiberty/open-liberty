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
public class IdClass0106 implements Serializable {

    private double entity0106_id1;

    private double entity0106_id2;

    public IdClass0106() {}

    public IdClass0106(double id1,
                       double id2) {
        this.entity0106_id1 = id1;
        this.entity0106_id2 = id2;
    }

    @Override
    public String toString() {
        return (" IdClass0106: " +
                " entity0106_id1: " + getEntity0106_id1() +
                " entity0106_id2: " + getEntity0106_id2());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0106))
            return false;
        if (o == this)
            return true;
        IdClass0106 idClass = (IdClass0106) o;
        return (idClass.entity0106_id1 == entity0106_id1 &&
                idClass.entity0106_id2 == entity0106_id2);
    }

    @Override
    public int hashCode() {
        int result = 0;
        Long long1 = Double.doubleToLongBits(entity0106_id1);
        Long long2 = Double.doubleToLongBits(entity0106_id2);
        result = result + (int) (long1 ^ (long1 >>> 32));
        result = result + (int) (long2 ^ (long2 >>> 32));
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public double getEntity0106_id1() {
        return entity0106_id1;
    }

    public double getEntity0106_id2() {
        return entity0106_id2;
    }
}

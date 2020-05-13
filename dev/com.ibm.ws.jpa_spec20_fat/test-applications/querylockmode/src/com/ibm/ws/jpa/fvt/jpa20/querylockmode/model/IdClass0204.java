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
public class IdClass0204 implements Serializable {

    private Character entity0204_id1;

    private Character entity0204_id2;

    private Character entity0204_id3;

    public IdClass0204() {}

    public IdClass0204(Character id1,
                       Character id2,
                       Character id3) {
        this.entity0204_id1 = id1;
        this.entity0204_id2 = id2;
        this.entity0204_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0204: " +
                " entity0204_id1: " + getEntity0204_id1() +
                " entity0204_id2: " + getEntity0204_id2() +
                " entity0204_id3: " + getEntity0204_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0204))
            return false;
        if (o == this)
            return true;
        IdClass0204 idClass = (IdClass0204) o;
        return (idClass.entity0204_id1 == entity0204_id1 &&
                idClass.entity0204_id2 == entity0204_id2 &&
                idClass.entity0204_id3 == entity0204_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0204_id1.hashCode();
        result = result + entity0204_id2.hashCode();
        result = result + entity0204_id3.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Character getEntity0204_id1() {
        return entity0204_id1;
    }

    public Character getEntity0204_id2() {
        return entity0204_id2;
    }

    public Character getEntity0204_id3() {
        return entity0204_id3;
    }
}

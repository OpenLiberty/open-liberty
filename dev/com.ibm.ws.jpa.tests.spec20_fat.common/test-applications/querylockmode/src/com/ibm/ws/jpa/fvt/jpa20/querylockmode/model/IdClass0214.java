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
public class IdClass0214 implements Serializable {

    private short entity0214_id1;

    private short entity0214_id2;

    private short entity0214_id3;

    public IdClass0214() {}

    public IdClass0214(short id1,
                       short id2,
                       short id3) {
        this.entity0214_id1 = id1;
        this.entity0214_id2 = id2;
        this.entity0214_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0214: " +
                " entity0214_id1: " + getEntity0214_id1() +
                " entity0214_id2: " + getEntity0214_id2() +
                " entity0214_id3: " + getEntity0214_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0214))
            return false;
        if (o == this)
            return true;
        IdClass0214 idClass = (IdClass0214) o;
        return (idClass.entity0214_id1 == entity0214_id1 &&
                idClass.entity0214_id2 == entity0214_id2 &&
                idClass.entity0214_id3 == entity0214_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0214_id1;
        result = result + entity0214_id2;
        result = result + entity0214_id3;
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public short getEntity0214_id1() {
        return entity0214_id1;
    }

    public short getEntity0214_id2() {
        return entity0214_id2;
    }

    public short getEntity0214_id3() {
        return entity0214_id3;
    }
}

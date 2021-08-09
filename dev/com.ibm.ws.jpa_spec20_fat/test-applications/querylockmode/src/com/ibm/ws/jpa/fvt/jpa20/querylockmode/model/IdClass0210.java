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
public class IdClass0210 implements Serializable {

    private int entity0210_id1;

    private int entity0210_id2;

    private int entity0210_id3;

    public IdClass0210() {}

    public IdClass0210(int id1,
                       int id2,
                       int id3) {
        this.entity0210_id1 = id1;
        this.entity0210_id2 = id2;
        this.entity0210_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0210: " +
                " entity0210_id1: " + getEntity0210_id1() +
                " entity0210_id2: " + getEntity0210_id2() +
                " entity0210_id3: " + getEntity0210_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0210))
            return false;
        if (o == this)
            return true;
        IdClass0210 idClass = (IdClass0210) o;
        return (idClass.entity0210_id1 == entity0210_id1 &&
                idClass.entity0210_id2 == entity0210_id2 &&
                idClass.entity0210_id3 == entity0210_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0210_id1;
        result = result + entity0210_id2;
        result = result + entity0210_id3;
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntity0210_id1() {
        return entity0210_id1;
    }

    public int getEntity0210_id2() {
        return entity0210_id2;
    }

    public int getEntity0210_id3() {
        return entity0210_id3;
    }
}

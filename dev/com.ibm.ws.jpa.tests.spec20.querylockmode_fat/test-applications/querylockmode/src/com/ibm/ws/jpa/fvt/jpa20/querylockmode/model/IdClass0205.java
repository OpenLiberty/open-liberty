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
public class IdClass0205 implements Serializable {

    private String entity0205_id1;

    private String entity0205_id2;

    private String entity0205_id3;

    public IdClass0205() {}

    public IdClass0205(String id1,
                       String id2,
                       String id3) {
        this.entity0205_id1 = id1;
        this.entity0205_id2 = id2;
        this.entity0205_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0205: " +
                " entity0205_id1: " + getEntity0205_id1() +
                " entity0205_id2: " + getEntity0205_id2() +
                " entity0205_id3: " + getEntity0205_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0205))
            return false;
        if (o == this)
            return true;
        IdClass0205 idClass = (IdClass0205) o;
        return (idClass.entity0205_id1 == entity0205_id1 &&
                idClass.entity0205_id2 == entity0205_id2 &&
                idClass.entity0205_id3 == entity0205_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0205_id1.hashCode();
        result = result + entity0205_id2.hashCode();
        result = result + entity0205_id3.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public String getEntity0205_id1() {
        return entity0205_id1;
    }

    public String getEntity0205_id2() {
        return entity0205_id2;
    }

    public String getEntity0205_id3() {
        return entity0205_id3;
    }
}

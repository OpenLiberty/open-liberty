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
import java.math.BigDecimal;

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
public class IdClass0335 implements Serializable {

    private BigDecimal entity0335_id1;

    private java.util.Date entity0335_id2;

    private java.sql.Date entity0335_id3;

    public IdClass0335() {}

    public IdClass0335(BigDecimal id1,
                       java.util.Date id2,
                       java.sql.Date id3) {
        this.entity0335_id1 = id1;
        this.entity0335_id2 = id2;
        this.entity0335_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0335: " +
                " entity0335_id1: " + getEntity0335_id1() +
                " entity0335_id2: " + getEntity0335_id2() +
                " entity0335_id3: " + getEntity0335_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0335))
            return false;
        if (o == this)
            return true;
        IdClass0335 idClass = (IdClass0335) o;
        return (idClass.entity0335_id1 == entity0335_id1 &&
                idClass.entity0335_id2 == entity0335_id2 &&
                idClass.entity0335_id3 == entity0335_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0335_id1.hashCode();
        result = result + entity0335_id2.hashCode();
        result = result + entity0335_id3.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public BigDecimal getEntity0335_id1() {
        return entity0335_id1;
    }

    public java.util.Date getEntity0335_id2() {
        return entity0335_id2;
    }

    public java.sql.Date getEntity0335_id3() {
        return entity0335_id3;
    }
}

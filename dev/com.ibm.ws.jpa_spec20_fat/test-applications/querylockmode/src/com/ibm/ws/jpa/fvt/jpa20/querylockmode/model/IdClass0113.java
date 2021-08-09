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
public class IdClass0113 implements Serializable {

    private Long entity0113_id1;

    private Long entity0113_id2;

    public IdClass0113() {}

    public IdClass0113(Long id1,
                       Long id2) {
        this.entity0113_id1 = id1;
        this.entity0113_id2 = id2;
    }

    @Override
    public String toString() {
        return (" IdClass0113: " +
                " entity0113_id1: " + getEntity0113_id1() +
                " entity0113_id2: " + getEntity0113_id2());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0113))
            return false;
        if (o == this)
            return true;
        IdClass0113 idClass = (IdClass0113) o;
        return (idClass.entity0113_id1 == entity0113_id1 &&
                idClass.entity0113_id2 == entity0113_id2);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0113_id1.hashCode();
        result = result + entity0113_id2.hashCode();
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public Long getEntity0113_id1() {
        return entity0113_id1;
    }

    public Long getEntity0113_id2() {
        return entity0113_id2;
    }
}

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
public class IdClass0112 implements Serializable {

    private long entity0112_id1;

    private long entity0112_id2;

    public IdClass0112() {}

    public IdClass0112(long id1,
                       long id2) {
        this.entity0112_id1 = id1;
        this.entity0112_id2 = id2;
    }

    @Override
    public String toString() {
        return (" IdClass0112: " +
                " entity0112_id1: " + getEntity0112_id1() +
                " entity0112_id2: " + getEntity0112_id2());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0112))
            return false;
        if (o == this)
            return true;
        IdClass0112 idClass = (IdClass0112) o;
        return (idClass.entity0112_id1 == entity0112_id1 &&
                idClass.entity0112_id2 == entity0112_id2);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + (int) (entity0112_id1 ^ (entity0112_id1 >>> 32));
        result = result + (int) (entity0112_id2 ^ (entity0112_id2 >>> 32));
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public long getEntity0112_id1() {
        return entity0112_id1;
    }

    public long getEntity0112_id2() {
        return entity0112_id2;
    }
}

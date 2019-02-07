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
public class IdClass0329 implements Serializable {

    private int entity0329_id1;

    private long entity0329_id2;

    private short entity0329_id3;

    public IdClass0329() {}

    public IdClass0329(int id1,
                       long id2,
                       short id3) {
        this.entity0329_id1 = id1;
        this.entity0329_id2 = id2;
        this.entity0329_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0329: " +
                " entity0329_id1: " + getEntity0329_id1() +
                " entity0329_id2: " + getEntity0329_id2() +
                " entity0329_id3: " + getEntity0329_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0329))
            return false;
        if (o == this)
            return true;
        IdClass0329 idClass = (IdClass0329) o;
        return (idClass.entity0329_id1 == entity0329_id1 &&
                idClass.entity0329_id2 == entity0329_id2 &&
                idClass.entity0329_id3 == entity0329_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0329_id1;
        Long long2 = Double.doubleToLongBits(entity0329_id2);
        result = result + (int) (long2 ^ (long2 >>> 32));
        result = result + entity0329_id3;
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public int getEntity0329_id1() {
        return entity0329_id1;
    }

    public long getEntity0329_id2() {
        return entity0329_id2;
    }

    public short getEntity0329_id3() {
        return entity0329_id3;
    }
}

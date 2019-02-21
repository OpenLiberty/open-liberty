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
public class IdClass0301 implements Serializable {

    private byte entity0301_id1;

    private Byte entity0301_id2;

    private char entity0301_id3;

    public IdClass0301() {}

    public IdClass0301(byte id1,
                       Byte id2,
                       char id3) {
        this.entity0301_id1 = id1;
        this.entity0301_id2 = id2;
        this.entity0301_id3 = id3;
    }

    @Override
    public String toString() {
        return (" IdClass0301: " +
                " entity0301_id1: " + getEntity0301_id1() +
                " entity0301_id2: " + getEntity0301_id2() +
                " entity0301_id3: " + getEntity0301_id3());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null)
            return false;
        if (!(o instanceof IdClass0301))
            return false;
        if (o == this)
            return true;
        IdClass0301 idClass = (IdClass0301) o;
        return (idClass.entity0301_id1 == entity0301_id1 &&
                idClass.entity0301_id2 == entity0301_id2 &&
                idClass.entity0301_id3 == entity0301_id3);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = result + entity0301_id1;
        result = result + entity0301_id2.hashCode();
        result = result + entity0301_id3;
        return result;
    }

    //----------------------------------------------------------------------------------------------
    // Persisent property accessor(s)
    //----------------------------------------------------------------------------------------------
    public byte getEntity0301_id1() {
        return entity0301_id1;
    }

    public Byte getEntity0301_id2() {
        return entity0301_id2;
    }

    public char getEntity0301_id3() {
        return entity0301_id3;
    }
}

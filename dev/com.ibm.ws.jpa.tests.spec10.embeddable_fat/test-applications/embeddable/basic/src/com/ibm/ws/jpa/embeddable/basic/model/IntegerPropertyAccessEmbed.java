/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.jpa.embeddable.basic.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
@Access(AccessType.PROPERTY)
public class IntegerPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<IntegerPropertyAccessEmbed> LIST_INIT = Arrays.asList(new IntegerPropertyAccessEmbed(new Integer(2)),
                                                                                   new IntegerPropertyAccessEmbed(new Integer(3)),
                                                                                   new IntegerPropertyAccessEmbed(new Integer(1)));
    public static final List<IntegerPropertyAccessEmbed> LIST_UPDATE = Arrays.asList(new IntegerPropertyAccessEmbed(new Integer(2)),
                                                                                     new IntegerPropertyAccessEmbed(new Integer(4)),
                                                                                     new IntegerPropertyAccessEmbed(new Integer(3)),
                                                                                     new IntegerPropertyAccessEmbed(new Integer(1)));

    public static final Set<IntegerPropertyAccessEmbed> SET_INIT = new HashSet<IntegerPropertyAccessEmbed>(LIST_INIT);

    public static final Map<Integer, IntegerPropertyAccessEmbed> MAP_INIT;
    static {
        Map<Integer, IntegerPropertyAccessEmbed> map = new HashMap<Integer, IntegerPropertyAccessEmbed>();
        map.put(new Integer(3), new IntegerPropertyAccessEmbed(new Integer(300)));
        map.put(new Integer(1), new IntegerPropertyAccessEmbed(new Integer(100)));
        map.put(new Integer(2), new IntegerPropertyAccessEmbed(new Integer(200)));
        MAP_INIT = Collections.unmodifiableMap(map);
    }

    private Integer integerValuePropertyAccessColumn;

    public IntegerPropertyAccessEmbed() {
    }

    public IntegerPropertyAccessEmbed(int integerValuePropertyAccessColumn) {
        this.integerValuePropertyAccessColumn = new Integer(integerValuePropertyAccessColumn);
    }

    @Column(name = "integerValuePAColumn")
    public Integer getIntegerValuePropertyAccessColumn() {
        return this.integerValuePropertyAccessColumn;
    }

    public void setIntegerValuePropertyAccessColumn(Integer integerValuePropertyAccessColumn) {
        this.integerValuePropertyAccessColumn = integerValuePropertyAccessColumn;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof IntegerPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValuePropertyAccessColumn=" + getIntegerValuePropertyAccessColumn();
    }

}

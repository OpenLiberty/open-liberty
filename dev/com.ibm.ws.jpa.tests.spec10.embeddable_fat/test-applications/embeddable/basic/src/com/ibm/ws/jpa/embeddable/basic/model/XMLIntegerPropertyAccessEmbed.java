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

public class XMLIntegerPropertyAccessEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<XMLIntegerPropertyAccessEmbed> LIST_INIT = Arrays.asList(new XMLIntegerPropertyAccessEmbed(new Integer(2)),
                                                                                      new XMLIntegerPropertyAccessEmbed(new Integer(3)),
                                                                                      new XMLIntegerPropertyAccessEmbed(new Integer(1)));
    public static final List<XMLIntegerPropertyAccessEmbed> LIST_UPDATE = Arrays.asList(new XMLIntegerPropertyAccessEmbed(new Integer(2)),
                                                                                        new XMLIntegerPropertyAccessEmbed(new Integer(4)),
                                                                                        new XMLIntegerPropertyAccessEmbed(new Integer(3)),
                                                                                        new XMLIntegerPropertyAccessEmbed(new Integer(1)));

    public static final Set<XMLIntegerPropertyAccessEmbed> SET_INIT = new HashSet<XMLIntegerPropertyAccessEmbed>(LIST_INIT);

    public static final Map<Integer, XMLIntegerPropertyAccessEmbed> MAP_INIT;
    static {
        Map<Integer, XMLIntegerPropertyAccessEmbed> map = new HashMap<Integer, XMLIntegerPropertyAccessEmbed>();
        map.put(new Integer(3), new XMLIntegerPropertyAccessEmbed(new Integer(300)));
        map.put(new Integer(1), new XMLIntegerPropertyAccessEmbed(new Integer(100)));
        map.put(new Integer(2), new XMLIntegerPropertyAccessEmbed(new Integer(200)));
        MAP_INIT = Collections.unmodifiableMap(map);
    }

    private Integer integerValuePropertyAccessColumn;

    public XMLIntegerPropertyAccessEmbed() {
    }

    public XMLIntegerPropertyAccessEmbed(int integerValuePropertyAccessColumn) {
        this.integerValuePropertyAccessColumn = new Integer(integerValuePropertyAccessColumn);
    }

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
        if (!(otherObject instanceof XMLIntegerPropertyAccessEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValuePropertyAccessColumn=" + getIntegerValuePropertyAccessColumn();
    }

}

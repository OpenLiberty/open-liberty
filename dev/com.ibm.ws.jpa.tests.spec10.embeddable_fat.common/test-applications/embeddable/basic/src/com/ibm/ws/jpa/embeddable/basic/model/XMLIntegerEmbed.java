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
import java.util.List;
import java.util.Map;

public class XMLIntegerEmbed implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final List<XMLIntegerEmbed> LIST_INIT = Arrays.asList(new XMLIntegerEmbed(new Integer(2)),
                                                                        new XMLIntegerEmbed(new Integer(3)),
                                                                        new XMLIntegerEmbed(new Integer(1)));
    public static final List<XMLIntegerEmbed> LIST_UPDATE = Arrays.asList(new XMLIntegerEmbed(new Integer(2)),
                                                                          new XMLIntegerEmbed(new Integer(4)),
                                                                          new XMLIntegerEmbed(new Integer(3)),
                                                                          new XMLIntegerEmbed(new Integer(1)));

    public static final Map<XMLIntegerEmbed, XMLIntegerEmbed> MAP_INIT2;
    static {
        Map<XMLIntegerEmbed, XMLIntegerEmbed> map = new HashMap<XMLIntegerEmbed, XMLIntegerEmbed>();
        map.put(new XMLIntegerEmbed(new Integer(3)), new XMLIntegerEmbed(new Integer(300)));
        map.put(new XMLIntegerEmbed(new Integer(1)), new XMLIntegerEmbed(new Integer(100)));
        map.put(new XMLIntegerEmbed(new Integer(2)), new XMLIntegerEmbed(new Integer(200)));
        MAP_INIT2 = Collections.unmodifiableMap(map);
    }

    private Integer integerValue;

    public XMLIntegerEmbed() {
    }

    public XMLIntegerEmbed(int integerValue) {
        this.integerValue = new Integer(integerValue);
    }

    public Integer getIntegerValue() {
        return this.integerValue;
    }

    public void setIntegerValue(Integer integerValue) {
        this.integerValue = integerValue;
    }

    @Override
    public int hashCode() {
        return (37 * 17 + toString().hashCode());
    }

    @Override
    public boolean equals(Object otherObject) {

        if (otherObject == this)
            return true;
        if (!(otherObject instanceof XMLIntegerEmbed))
            return false;
        return (otherObject.hashCode() == hashCode());
    }

    @Override
    public String toString() {
        return "integerValue=" + integerValue;
    }
}
